package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj, EvalContext> {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    // Create a top-level scope.
    mGlobalScope = new Scope();
    
    // Create the built-in objects. These objects essentially define a class
    // system in terms of Magpie's core prototype-based runtime. The diagram
    // below shows the object hierarchy for the built-in objects.
    //
    // Root
    //  +-- BoolProto
    //  |    +-- true
    //  |    +-- false
    //  +-- ClassProto
    //  |    +-- BoolClass
    //  |    +-- ClassClass
    //  |    +-- FnClass
    //  |    +-- IntClass
    //  |    +-- NothingClass
    //  |    +-- StringClass
    //  |    +-- TupleClass
    //  +-- FnProto
    //  +-- IntProto
    //  |    +-- int instances...
    //  +-- Nothing
    //  +-- StringProto
    //  |    +-- string instances...
    //  +-- TupleProto
    //
    // In addition, each of the ___Proto object has a field named "class" that
    // refers to its corresponding ___Class object. Likewise, the ___Class
    // objects have a reference to the ___Proto objects that they use when
    // constructing new instances of their class (except for NothingClass).
    
    mRoot = new Obj();
    
    mClassProto = mRoot.spawn();
    Obj classClass = mRoot.spawn();
    classClass.add("proto", mClassProto);
    mClassProto.add("class", classClass);
    mClassProto.add("new", ClassMethods.newObj());
    
    Obj nothingClass = mClassProto.spawn();
    mNothing = mRoot.spawn();
    mNothing.add("class", nothingClass);

    Obj boolClass = mClassProto.spawn();
    mBoolProto = mRoot.spawn();
    boolClass.add("proto", mBoolProto);
    mBoolProto.add("class", boolClass);
    mBoolProto.add("toString", BoolMethods.toStringMethod());
    
    Obj fnClass = mClassProto.spawn();
    mFnProto = mRoot.spawn();
    fnClass.add("proto", mFnProto);
    mFnProto.add("class", fnClass);
    
    Obj intClass = mClassProto.spawn();
    mIntProto = mRoot.spawn();
    intClass.add("proto", mIntProto);
    mIntProto.add("class", intClass);
    mIntProto.add("+", IntMethods.operatorPlus());
    mIntProto.add("-", IntMethods.operatorMinus());
    mIntProto.add("*", IntMethods.operatorMultiply());
    mIntProto.add("/", IntMethods.operatorDivide());
    mIntProto.add("toString", IntMethods.toStringMethod());
    mIntProto.add("==", IntMethods.operatorEqual());
    mIntProto.add("!=", IntMethods.operatorNotEqual());
    mIntProto.add("<",  IntMethods.operatorLessThan());
    mIntProto.add(">",  IntMethods.operatorGreaterThan());
    mIntProto.add("<=", IntMethods.operatorLessThanOrEqual());
    mIntProto.add(">=", IntMethods.operatorGreaterThanOrEqual());

    Obj stringClass = mClassProto.spawn();
    mStringProto = mRoot.spawn();
    stringClass.add("proto", mStringProto);
    mStringProto.add("class", stringClass);
    mStringProto.add("+",     StringMethods.operatorPlus());
    mStringProto.add("print", StringMethods.print());

    // TODO(bob): At some point, may want different tuple types based on the
    // types of the fields.
    Obj tupleClass = mClassProto.spawn();
    mTupleProto = mRoot.spawn();
    tupleClass.add("proto", mTupleProto);
    mTupleProto.add("class", tupleClass);
    
    // Give the classes names and make then available.
    mGlobalScope.define("Bool", boolClass);
    mGlobalScope.define("Function", fnClass);
    mGlobalScope.define("Int", intClass);
    mGlobalScope.define("String", stringClass);
    mGlobalScope.define("Tuple", tupleClass);

    boolClass.add("name", createString("Bool"));
    classClass.add("name", createString("Class"));
    fnClass.add("name", createString("Function"));
    intClass.add("name", createString("Int"));
    nothingClass.add("name", createString("Nothing"));
    stringClass.add("name", createString("String"));
    tupleClass.add("name", createString("Tuple"));
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  public void run(List<Expr> expressions) {
    EvalContext context = new EvalContext(mGlobalScope, mNothing);
    
    // First, evaluate the expressions. This is the load time evaluation.
    for (Expr expr : expressions) {
      evaluate(expr, context);
    }
    
    // TODO(bob): Type-checking and static analysis goes here.
    
    // Now, if there is a main(), call it. This is the runtime.
    Obj main = context.lookUp("main");
    if (main == null) return;
    
    if (!(main instanceof FnObj)) throw new InterpreterException("main is not a function.");
    
    FnObj mainFn = (FnObj)main;
    invoke(mNothing, mainFn.getFunction(), nothing());
  }
  
  public void print(String text) {
    mHost.print(text);
  }
  
  /**
   * Gets the single value () of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public Obj createBool(boolean value) {
    return mBoolProto.spawn(value);
  }

  public Obj createInt(int value) {
    return mIntProto.spawn(value);
  }
  
  public Obj createString(String value) {
    return mStringProto.spawn(value);
  }
  
  public Obj invoke(Obj thisRef, FnExpr function, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = new EvalContext(new Scope(mGlobalScope), thisRef);
    
    // Bind arguments to their parameter names.
    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), arg.getField(Integer.toString(i)));
      }
    }
    
    return evaluate(function.getBody(), context);
  }
  
  private Obj invokeMethod(EvalContext context, Obj thisObj, String name, Obj arg) {
    // Look up the method.
    Invokable method = thisObj.getMethod(name);
    if (method != null) {
      EvalContext thisContext = context.bindThis(thisObj);
      return method.invoke(this, thisContext, arg);
    }
    
    // If there is no arg, look for a field.
    if (arg == mNothing) {
      Obj field = thisObj.getField(name);
      if (field != null) return field;
    }

    throw new InterpreterException("Could not find a member \"" + name + "\" on " + thisObj);
  }
  
  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);
    context.assign(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    EvalContext localContext = context.newScope();
    
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
    // Given a call expression like "foo bar", we look for the following in
    // order until we find a match.
    // 1. Look for a variable named "foo".
    //    (The type checker will ensure "foo" is a function that takes "bar"'s
    //    type.)
    // TODO(bob): Implement this case once we have function objects and
    //            variables.
    // 2. Look for a method "foo" on the type of the argument "bar".
    
    Obj arg = evaluate(expr.getArg(), context);
    
    // Handle a named target.
    if (expr.getTarget() instanceof NameExpr) {
      String name = ((NameExpr)expr.getTarget()).getName();
      
      // Look for a local variable with the name.
      Obj local = context.lookUp(name);
      if (local != null) {
        if (!(local instanceof FnObj)) {
          throw new InterpreterException("Can not call a local variable that does not contain a function.");
        }
        
        // TODO(bob): May want to support a more generic callable interface
        // eventually.
        FnObj function = (FnObj)local;
        return invoke(mNothing, function.getFunction(), arg);
      }
      
      // Look for an implicit call to a method on this with the name.
      Invokable method = context.getThis().getMethod(name);
      if (method != null) {
        return invokeMethod(context, context.getThis(), name, arg);
      }
      
      // Try to call it as a method on the argument. In other words,
      // "abs 123" is equivalent to "123.abs".
      return invokeMethod(context, arg, name, nothing());
    }
    
    // Not an explicit named target, so evaluate it and see if it's callable.
    Obj target = evaluate(expr.getTarget(), context);
    
    if (!(target instanceof FnObj)) {
      throw new InterpreterException("Can not call an expression that does not evaluate to a function.");
    }

    FnObj targetFn = (FnObj)target;
    return invoke(mNothing, targetFn.getFunction(), arg);
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // Create a class object with the shared properties.
    Obj classObj = mClassProto.spawn();
    classObj.add("name", createString(expr.getName()));
    
    // TODO(bob): Need to add constructors here...
    
    // Create an instance prototype with the instance methods.
    Obj proto = mRoot.spawn();
    classObj.add("proto", proto);

    /*
    // Define the fields.
    for (String field : expr.getFields().keySet()) {
      classObj.getInstanceFields().put(field, true);
    }
    */
    
    // Define the methods.
    for (Entry<String, FnExpr> entry : expr.getMethods().entrySet()) {
      Method method = new Method(entry.getValue());
      proto.add(entry.getKey(), method);
    }
    
    context.define(expr.getName(), classObj);
    return classObj;
  }

  @Override
  public Obj visit(DefineExpr expr, EvalContext context) {
    // TODO(bob): need to handle mutability
    Obj value = evaluate(expr.getValue(), context);
    context.define(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    return new FnObj(mFnProto, expr);
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
    
    // See if there's a method on this with the name.
    Invokable method = context.getThis().getMethod(expr.getName());
    if (method != null) {
      return invokeMethod(context, context.getThis(), expr.getName(), mNothing);
    }
    
    throw new InterpreterException("Could not find a variable or member named \"" + expr.getName() + "\".");
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
    // A tuple is an object with fields whose names are zero-based numbers.
    
    Obj tuple = mTupleProto.spawn();
    for (int i = 0; i < expr.getFields().size(); i++) {
      tuple.add(Integer.toString(i), evaluate(expr.getFields().get(i), context));
    }
    
    return tuple;
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  private final Obj mRoot;
  private final Obj mNothing;
  private final Obj mBoolProto;
  private final Obj mClassProto;
  private final Obj mFnProto;
  private final Obj mIntProto;
  private final Obj mStringProto;
  private final Obj mTupleProto;
}
