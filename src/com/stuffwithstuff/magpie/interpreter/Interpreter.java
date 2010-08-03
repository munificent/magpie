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
    mClassClass.addInstanceMember("addMethod", new NativeMethodObj.ClassAddMethod());
    mClassClass.addInstanceMember("addSharedMethod", new NativeMethodObj.ClassAddSharedMethod());
    mClassClass.addInstanceMember("new", new NativeMethodObj.ClassNew());

    mBoolClass = new ClassObj(mClassClass);
    mBoolClass.addInstanceMember("not", new NativeMethodObj.BoolNot());
    mBoolClass.addInstanceMember("toString", new NativeMethodObj.BoolToString());

    mFnClass = new ClassObj(mClassClass);
    
    mIntClass = new ClassObj(mClassClass);
    mIntClass.addInstanceMember("+", new NativeMethodObj.IntPlus());
    mIntClass.addInstanceMember("-", new NativeMethodObj.IntMinus());
    mIntClass.addInstanceMember("*", new NativeMethodObj.IntMultiply());
    mIntClass.addInstanceMember("/", new NativeMethodObj.IntDivide());
    mIntClass.addInstanceMember("toString", new NativeMethodObj.IntToString());
    mIntClass.addInstanceMember("==", new NativeMethodObj.IntEqual());
    mIntClass.addInstanceMember("!=", new NativeMethodObj.IntNotEqual());
    mIntClass.addInstanceMember("<",  new NativeMethodObj.IntLessThan());
    mIntClass.addInstanceMember(">",  new NativeMethodObj.IntGreaterThan());
    mIntClass.addInstanceMember("<=", new NativeMethodObj.IntLessThanOrEqual());
    mIntClass.addInstanceMember(">=", new NativeMethodObj.IntGreaterThanOrEqual());

    mStringClass = new ClassObj(mClassClass);
    mStringClass.addInstanceMember("+",     new NativeMethodObj.StringPlus());
    mStringClass.addInstanceMember("print", new NativeMethodObj.StringPrint());

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

    mBoolClass.add("name", createString("Bool"));
    mClassClass.add("name", createString("Class"));
    mFnClass.add("name", createString("Function"));
    mIntClass.add("name", createString("Int"));
    nothingClass.add("name", createString("Nothing"));
    mStringClass.add("name", createString("String"));
    mTupleClass.add("name", createString("Tuple"));
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
    invoke(mNothing, mainFn.getFunction(), nothing());
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
      tuple.add(Integer.toString(i), fields[i]);
    }
    
    return tuple;
  }
  
  public Obj invoke(Obj thisRef, FnExpr function, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = EvalContext.forMethod(mGlobalScope, thisRef);
    
    // Bind arguments to their parameter names.
    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        // TODO(bob): What happens if this member is a method?
        context.define(params.get(i), arg.getMember(Integer.toString(i)));
      }
    }
    
    return evaluate(function.getBody(), context);
  }
  
  private Obj invokeMethod(EvalContext context, Obj thisObj, String name, Obj arg) {
    Obj member = thisObj.getMember(name);
    
    expect(member != null, "Could not find a member \"%s\" on %s.",
        name, thisObj);
    
    if (member instanceof Invokable) {
      // It's a method, so invoke it.
      Invokable method = (Invokable)member;
      EvalContext thisContext = context.bindThis(thisObj);
      return method.invoke(this, thisContext, arg);
    } else {
      // If the member isn't callable and we aren't passing an arg, it's a
      // field, so just return it.
      if (arg == mNothing) {
        return member;
      } else {
        throw failure("Member \"%s\" on %s is not invokable.", name, thisObj);
      }
    }
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
      if (context.getThis().assign(name, value)) return value;
      
      throw failure("Couldn't find a variable or member \"%s\" to assign to.", name);
    } else {
      // The target of the assignment is an actual expression, like a.b = c
      Obj target = evaluate(expr.getTarget(), context);
      
      // Look for a setter method.
      String setterName = expr.getName() + "=";
      Obj setter = target.getMember(setterName);
      if (setter != null) {
        expect(setter instanceof Invokable,
            "Cannot use a field named \"%s\" as an assignment target.",
            setterName);

        Obj value = evaluate(expr.getValue(), context);
        
        // If the assignment statement has an argument and a value, like:
        // a.b c = v (c is the arg, v is the value)
        // then bundle them together:
        if (expr.getTargetArg() != null) {
          Obj targetArg = evaluate(expr.getTargetArg(), context);
          value = createTuple(context, targetArg, value);
        }
        
        // Invoke the setter.
        return invokeMethod(context, target, setterName, value);
      }
      
      // Try to assign a field with the right name.
      if (expr.getTargetArg() == null) {
        Obj value = evaluate(expr.getValue(), context);
        if (target.assign(expr.getName(), value)) return value;
      }
      
      throw failure("Could not find a setter or member named \"%s\" on %s to assign to.",
        expr.getName(), target);
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
        return function.invoke(this, context, arg);
      }
      
      // Look for an implicit call to a method on this with the name.
      Obj member = context.getThis().getMember(name);
      if (member != null) {
        expect(member instanceof Invokable,
            "Member \"%s\" of %s cannot be invoked.", name, context.getThis());
        
        return invokeMethod(context, context.getThis(), name, arg);
      }
      
      // Try to call it as a method on the argument. In other words,
      // "abs 123" is equivalent to "123.abs".
      return invokeMethod(context, arg, name, nothing());
    }
    
    // Not an explicit named target, so evaluate it and see if it's callable.
    Obj target = evaluate(expr.getTarget(), context);
    
    expect(target instanceof FnObj,
        "Can not call an expression that does not evaluate to a function.");

    FnObj targetFn = (FnObj)target;
    return invoke(mNothing, targetFn.getFunction(), arg);
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // Create a class object with the shared properties.
    ClassObj classObj = new ClassObj(mClassClass);
    classObj.add("name", createString(expr.getName()));
    
    // Evaluate and define the shared fields.
    EvalContext classContext = context.bindThis(classObj);
    for (Entry<String, Expr> field : expr.getSharedFields().entrySet()) {
      Obj value = evaluate(field.getValue(), classContext);
      classObj.add(field.getKey(), value);
    }
    
    // Define the shared methods.
    for (Entry<String, FnExpr> method : expr.getSharedMethods().entrySet()) {
      FnObj methodObj = new FnObj(mFnClass, method.getValue());
      classObj.add(method.getKey(), methodObj);
    }
    
    // Define the instance methods.
    for (Entry<String, FnExpr> method : expr.getMethods().entrySet()) {
      FnObj methodObj = new FnObj(mFnClass, method.getValue());
      classObj.addInstanceMember(method.getKey(), methodObj);
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
    
    return invokeMethod(context, receiver, expr.getMethod(), arg);
  }

  @Override
  public Obj visit(NameExpr expr, EvalContext context) {
    // Look up a named variable.
    Obj variable = context.lookUp(expr.getName());
    if (variable != null) return variable;
    
    // See if there's a member of this with the name.
    Obj member = context.getThis().getMember(expr.getName());
    expect(member != null, 
        "Could not find a variable or member named \"%s\".",
        expr.getName());
    
    // If it's a method, implicitly invoke it with nothing.
    // TODO(bob): Do we need to distinguish between method members and fields
    // whose value is a function?
    if (member instanceof Invokable) {
      return invokeMethod(context, context.getThis(), expr.getName(), mNothing);
    } else {
      // Just a field, so return it.
      return member;
    }
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
  
  /*
  private final Obj mRoot;
  */
  private final Obj mNothing;
  /*
  private final Obj mBoolProto;
  private final Obj mClassProto;
  private final Obj mFnProto;
  private final Obj mIntProto;
  private final Obj mStringProto;
  private final Obj mTupleProto;
  */
}
