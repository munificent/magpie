package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj, EvalContext> {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    // Create a top-level scope.
    mGlobalScope = new Scope();
    
    mClassClass = new ClassObj();
    mClassClass.addInstanceMethod("addMethod", new NativeMethodObj.ClassAddMethod());
    mClassClass.addInstanceMethod("addSharedMethod", new NativeMethodObj.ClassAddSharedMethod());
    mClassClass.addInstanceMethod("name", new NativeMethodObj.ClassFieldGetter("name"));
    mClassClass.addInstanceMethod("new", new NativeMethodObj.ClassNew());

    mBoolClass = new ClassObj(mClassClass);
    mBoolClass.addInstanceMethod("not", new NativeMethodObj.BoolNot());
    mBoolClass.addInstanceMethod("toString", new NativeMethodObj.BoolToString());

    mFnClass = new ClassObj(mClassClass);
    
    mIntClass = new ClassObj(mClassClass);
    mIntClass.addInstanceMethod("+", new NativeMethodObj.IntPlus());
    mIntClass.addInstanceMethod("-", new NativeMethodObj.IntMinus());
    mIntClass.addInstanceMethod("*", new NativeMethodObj.IntMultiply());
    mIntClass.addInstanceMethod("/", new NativeMethodObj.IntDivide());
    mIntClass.addInstanceMethod("toString", new NativeMethodObj.IntToString());
    mIntClass.addInstanceMethod("==", new NativeMethodObj.IntEqual());
    mIntClass.addInstanceMethod("!=", new NativeMethodObj.IntNotEqual());
    mIntClass.addInstanceMethod("<",  new NativeMethodObj.IntLessThan());
    mIntClass.addInstanceMethod(">",  new NativeMethodObj.IntGreaterThan());
    mIntClass.addInstanceMethod("<=", new NativeMethodObj.IntLessThanOrEqual());
    mIntClass.addInstanceMethod(">=", new NativeMethodObj.IntGreaterThanOrEqual());

    mStringClass = new ClassObj(mClassClass);
    mStringClass.addInstanceMethod("+",     new NativeMethodObj.StringPlus());
    mStringClass.addInstanceMethod("print", new NativeMethodObj.StringPrint());

    // TODO(bob): At some point, may want different tuple types based on the
    // types of the fields.
    mTupleClass = new ClassObj(mClassClass);
    
    ClassObj nothingClass = new ClassObj(mClassClass);
    mNothing = new Obj(nothingClass);
    
    // Give the classes names and make then available.
    mGlobalScope.define("Bool", mBoolClass);
    mGlobalScope.define("Function", mFnClass);
    mGlobalScope.define("Int", mIntClass);
    mGlobalScope.define("String", mStringClass);
    mGlobalScope.define("Tuple", mTupleClass);

    mBoolClass.setField("name", createString("Bool"));
    mClassClass.setField("name", createString("Class"));
    mFnClass.setField("name", createString("Function"));
    mIntClass.setField("name", createString("Int"));
    nothingClass.setField("name", createString("Nothing"));
    mStringClass.setField("name", createString("String"));
    mTupleClass.setField("name", createString("Tuple"));
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  public void run(List<Expr> expressions) {
    EvalContext context = EvalContext.topLevel(mGlobalScope, mNothing);
    
    // First, evaluate the expressions. This is the load time evaluation.
    for (Expr expr : expressions) {
      evaluate(expr, context);
    }
    
    // TODO(bob): Type-checking and static analysis goes here.
    
    // Now, if there is a main(), call it. This is the runtime.
    Obj main = context.lookUp("main");
    if (main == null) return;
    
    expect(main instanceof FnObj, "Member \"main\" is not a function.");
    
    FnObj mainFn = (FnObj)main;
    mainFn.invoke(this, mNothing, mNothing);
  }
  
  public void print(String text) {
    mHost.print(text);
  }
  
  public Scope getGlobals() { return mGlobalScope; }
  
  /**
   * Gets the single value () of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public Obj createBool(boolean value) {
    return mBoolClass.instantiate(value);
  }

  public Obj createInt(int value) {
    return mIntClass.instantiate(value);
  }
  
  public Obj createString(String value) {
    return mStringClass.instantiate(value);
  }
  
  public Obj createTuple(EvalContext context, Obj... fields) {
    // A tuple is an object with fields whose names are zero-based numbers.
    Obj tuple = mTupleClass.instantiate();
    for (int i = 0; i < fields.length; i++) {
      String name = Integer.toString(i);
      tuple.setField(name, fields[i]);
      tuple.addMethod(name, new NativeMethodObj.ClassFieldGetter(name));
    }
    
    return tuple;
  }
    
  public Obj invoke(Obj thisObj, FnExpr function, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = EvalContext.forMethod(mGlobalScope, thisObj);
    
    // Bind arguments to their parameter names.
    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), arg.getTupleField(i));
      }
    }
    
    return evaluate(function.getBody(), context);
  }
  
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    if (expr.getTarget() == null) {
      // No target means we're just assigning to a variable (or field of this)
      // with the given name.
      String name = expr.getName();
      Obj value = evaluate(expr.getValue(), context);
      
      // Try to assign to a local.
      if (context.assign(name, value)) return value;
      
      // If not found, try to assign to a member of this.
      Invokable setter = context.getThis().findMethod(name + "=", value);
      if (setter != null) {
        return setter.invoke(this, context.getThis(), value);
      }
      
      throw failure("Couldn't find a variable or member \"%s\" to assign to.", name);
    } else {
      // The target of the assignment is an actual expression, like a.b = c
      Obj target = evaluate(expr.getTarget(), context);
      Obj value = evaluate(expr.getValue(), context);

      // If the assignment statement has an argument and a value, like:
      // a.b c = v (c is the arg, v is the value)
      // then bundle them together:
      if (expr.getTargetArg() != null) {
        Obj targetArg = evaluate(expr.getTargetArg(), context);
        value = createTuple(context, targetArg, value);
      }

      // Look for a setter method.
      String setterName = expr.getName() + "=";
      Invokable setter = target.findMethod(setterName, value);
      
      expect(setter != null,
          "Could not find a method named \"%s\" on %s.", setterName, target);
      
      // Invoke the setter.
      return setter.invoke(this, target, value);
    }
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    EvalContext localContext = context.newBlockScope();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr, localContext);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return createBool(expr.getValue());
  }

  @Override
  public Obj visit(CallExpr expr, EvalContext context) {
    Obj arg = evaluate(expr.getArg(), context);
    
    // Handle a named target.
    if (expr.getTarget() instanceof NameExpr) {
      String name = ((NameExpr)expr.getTarget()).getName();
      
      // Look for a local variable with the name.
      Obj local = context.lookUp(name);
      if (local != null) {
        expect(local instanceof Invokable,
            "Can not call a local variable that does not contain a function.");
        
        Invokable function = (Invokable)local;
        return function.invoke(this, mNothing, arg);
      }
      
      // Look for an implicit call to a method on this with the name.
      Invokable method = context.getThis().findMethod(name, arg);
      if (method != null) {
        return method.invoke(this, context.getThis(), arg);
      }
      
      // Try to call it as a method on the argument. In other words,
      // "abs 123" is equivalent to "123.abs".
      method = arg.findMethod(name, mNothing);
      expect(method != null,
          "Could not find a method \"%s\" on %s.", name, arg);

      return method.invoke(this, arg, mNothing);
    }
    
    // Not an explicit named target, so evaluate it and see if it's callable.
    Obj target = evaluate(expr.getTarget(), context);
    
    expect(target instanceof FnObj,
        "Can not call an expression that does not evaluate to a function.");

    FnObj targetFn = (FnObj)target;
    return targetFn.invoke(this, mNothing, arg);
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // Create a class object with the shared properties.
    ClassObj classObj = new ClassObj(mClassClass);
    classObj.setField("name", createString(expr.getName()));
    
    // Add the constructors.
    for (FnExpr constructorFn : expr.getConstructors()) {
      FnObj fnObj = new FnObj(mFnClass, constructorFn);
      classObj.addConstructor(fnObj);
    }
    
    // Evaluate and define the shared fields.
    EvalContext classContext = context.bindThis(classObj);
    for (Entry<String, Expr> field : expr.getSharedFields().entrySet()) {
      Obj value = evaluate(field.getValue(), classContext);
      
      classObj.setField(field.getKey(), value);
      
      // Add a getter.
      classObj.addMethod(field.getKey(),
          new NativeMethodObj.ClassFieldGetter(field.getKey()));
      
      // Add a setter.
      classObj.addMethod(field.getKey() + "=",
          new NativeMethodObj.ClassFieldSetter(field.getKey()));
    }
    
    // Define the shared methods.
    for (Entry<String, FnExpr> method : expr.getSharedMethods().entrySet()) {
      FnObj methodObj = new FnObj(mFnClass, method.getValue());
      classObj.addMethod(method.getKey(), methodObj);
    }
    
    // Define the instance methods.
    for (Entry<String, FnExpr> method : expr.getMethods().entrySet()) {
      FnObj methodObj = new FnObj(mFnClass, method.getValue());
      classObj.addInstanceMethod(method.getKey(), methodObj);
    }
    
    // Define the getters and setters for the fields.
    for (String field : expr.getFields().keySet()) {
      // Add a getter.
      classObj.addInstanceMethod(field,
          new NativeMethodObj.ClassFieldGetter(field));
      
      // Add a setter.
      classObj.addInstanceMethod(field + "=",
          new NativeMethodObj.ClassFieldSetter(field));
    }
    
    for (String field : expr.getFieldDeclarations().keySet()) {
      // Add a getter.
      classObj.addInstanceMethod(field,
          new NativeMethodObj.ClassFieldGetter(field));
      
      // Add a setter.
      classObj.addInstanceMethod(field + "=",
          new NativeMethodObj.ClassFieldSetter(field));
    }
    
    // Add the field initializers to the class so it can evaluate them when an
    // object is constructed.
    classObj.defineFields(expr.getFields());
    
    // TODO(bob): Need to add constructors here...
    
    // Define a variable for the class in the current scope.
    context.define(expr.getName(), classObj);
    return classObj;
  }

  @Override
  public Obj visit(DefineExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);

    context.define(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    return new FnObj(mFnClass, expr);
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Evaluate all of the conditions.
    boolean passed = true;
    for (Expr condition : expr.getConditions()) {
      Obj result = evaluate(condition, context);
      if (!((Boolean)result.getPrimitiveValue()).booleanValue()) {
        // Condition failed.
        passed = false;
        break;
      }
    }
    
    // Evaluate the body.
    if (passed) {
      return evaluate(expr.getThen(), context);
    } else {
      return evaluate(expr.getElse(), context);
    }
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return createInt(expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    boolean done = false;
    while (true) {
      // Evaluate the conditions.
      for (Expr conditionExpr : expr.getConditions()) {
        // See if the while clause is still true.
        Obj condition = evaluate(conditionExpr, context);
        if (((Boolean)condition.getPrimitiveValue()).booleanValue() != true) {
          done = true;
          break;
        }
      }
      
      // If any clause failed, stop the loop.
      if (done) break;
      
      evaluate(expr.getBody(), context);
    }
    
    // TODO(bob): It would be cool if loops could have "else" clauses and then
    // reliably return a value.
    return nothing();
  }

  @Override
  public Obj visit(MethodExpr expr, EvalContext context) {
    Obj receiver = evaluate(expr.getReceiver(), context);
    Obj arg = evaluate(expr.getArg(), context);
    
    Invokable method = receiver.findMethod(expr.getMethod(), arg);
    expect (method != null,
        "Could not find a method named \"%s\" on %s.",
        expr.getMethod(), receiver);
    

    return method.invoke(this, receiver, arg);
  }

  @Override
  public Obj visit(NameExpr expr, EvalContext context) {
    // Look up a named variable.
    Obj variable = context.lookUp(expr.getName());
    if (variable != null) return variable;
    
    Invokable method = context.getThis().findMethod(expr.getName(), mNothing);
    expect (method != null,
        "Could not find a variable named \"%s\".",
        expr.getName());
    
    return method.invoke(this, context.getThis(), mNothing);
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mNothing;
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return createString(expr.getValue());
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    return context.getThis();
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // Evaluate the fields.
    Obj[] fields = new Obj[expr.getFields().size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = evaluate(expr.getFields().get(i), context);
    }

    return createTuple(context, fields);
  }
  
  private void expect(boolean condition, String format, Object... args) {
    if (!condition) {
      throw failure(format, args);
    }
  }
  
  /**
   * Returns a new interpreter exception. It should be called like:
   * 
   *    throw failure(...);
   * 
   * Note that this *returns* an exception instead of throwing it so that you
   * can use it in places where the Java compiler is doing reachability
   * analysis. For example, you can do "throw failure(...)" in the last line of
   * a function with a non-void return type and Java will allow it. If fail did
   * the throw internally, it would have no way of knowing the function doesn't
   * return.
   */
  private InterpreterException failure(String format, Object... args) {
    String message = String.format(format, args);
    return new InterpreterException(message);
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  
  private final ClassObj mClassClass;
  private final ClassObj mBoolClass;
  private final ClassObj mFnClass;
  private final ClassObj mIntClass;
  private final ClassObj mStringClass;
  private final ClassObj mTupleClass;
  
  private final Obj mNothing;
}
