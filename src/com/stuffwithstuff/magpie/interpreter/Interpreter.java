package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj> {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    // Create a top-level scope.
    mGlobalScope = new Scope();
    mScope = mGlobalScope;
    
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
    mScope.define("Bool", boolClass);
    mScope.define("Function", fnClass);
    mScope.define("Int", intClass);
    mScope.define("String", stringClass);
    mScope.define("Tuple", tupleClass);

    boolClass.add("name", createString("Bool"));
    classClass.add("name", createString("Class"));
    fnClass.add("name", createString("Function"));
    intClass.add("name", createString("Int"));
    nothingClass.add("name", createString("Nothing"));
    stringClass.add("name", createString("String"));
    tupleClass.add("name", createString("Tuple"));
  }
  
  public Obj evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  public void run(List<Expr> expressions) {
    // First, evaluate the expressions. This is the load time evaluation.
    for (Expr expr : expressions) {
      evaluate(expr);
    }
    
    // TODO(bob): Type-checking and static analysis goes here.
    
    // Now, if there is a main(), call it. This is the runtime.
    Obj main = mScope.get("main");
    if (main == null) return;
    
    if (!(main instanceof FnObj)) throw new InterpreterException("main is not a function.");
    
    FnObj mainFn = (FnObj)main;
    invokeInCurrentScope(mainFn.getParamNames(), mainFn.getBody(), nothing());
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
  
  public Obj invoke(FunctionDefn function, Obj arg) {
    return invoke(function.getParamNames(), function.getBody(), arg);
  }
  
  public Obj invoke(List<String> paramNames, Expr body, Obj arg) {
    // Create a new local scope for the function.
    Scope oldScope = mScope;
    mScope = new Scope(mGlobalScope);
    
    Obj result = invokeInCurrentScope(paramNames, body, arg);
    
    // Restore the previous scope.
    mScope = oldScope;
    
    return result;
  }
  
  private Obj invokeInCurrentScope(List<String> paramNames, Expr body, Obj arg) {
    // Create a new local scope for the function.
    Scope oldScope = mScope;
    mScope = new Scope(mGlobalScope);
    
    // Bind arguments to their parameter names.
    if (paramNames.size() == 1) {
      mScope.define(paramNames.get(0), arg);
    } else if (paramNames.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < paramNames.size(); i++) {
        mScope.define(paramNames.get(i), arg.getField(Integer.toString(i)));
      }
    }
    
    Obj result = evaluate(body);
    
    // Restore the previous scope.
    mScope = oldScope;
    
    return result;
  }
  
  @Override
  public Obj visit(AssignExpr expr) {
    Obj value = evaluate(expr.getValue());
    mScope.assign(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(BlockExpr expr) {
    Obj result = null;
    
    // Create a lexical scope.
    mScope = mScope.push();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr);
    }
    
    mScope = mScope.pop();
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr) {
    return createBool(expr.getValue());
  }

  @Override
  public Obj visit(CallExpr expr) {
    // Given a call expression like "foo bar", we look for the following in
    // order until we find a match.
    // 1. Look for a variable named "foo".
    //    (The type checker will ensure "foo" is a function that takes "bar"'s
    //    type.)
    // TODO(bob): Implement this case once we have function objects and
    //            variables.
    // 2. Look for a method "foo" on the type of the argument "bar".
    
    Obj arg = evaluate(expr.getArg());
    
    // Handle a named target.
    if (expr.getTarget() instanceof NameExpr) {
      String name = ((NameExpr)expr.getTarget()).getName();
      
      // Look for a local variable with the name.
      Obj local = mScope.get(name);
      if (local != null) {
        if (!(local instanceof FnObj)) {
          throw new InterpreterException("Can not call a local variable that does not contain a function.");
        }
        
        // TODO(bob): May want to support a more generic callable interface
        // eventually.
        FnObj function = (FnObj)local;
        return invoke(function.getParamNames(), function.getBody(), arg);
      }
      
      // Try to call it as a method on the argument. In other words,
      // "abs 123" is equivalent to "123.abs".
      return invokeMethod(name, arg, nothing());
    }
    
    // Not an explicit named target, so evaluate it and see if it's callable.
    Obj target = evaluate(expr.getTarget());
    
    if (!(target instanceof FnObj)) {
      throw new InterpreterException("Can not call an expression that does not evaluate to a function.");
    }

    FnObj targetFn = (FnObj)target;
    return invoke(targetFn.getParamNames(), targetFn.getBody(), arg);
  }

  @Override
  public Obj visit(ClassExpr expr) {
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
    
    mScope.define(expr.getName(), classObj);
    return classObj;
  }

  @Override
  public Obj visit(DefineExpr expr) {
    // TODO(bob): need to handle mutability
    Obj value = evaluate(expr.getValue());
    mScope.define(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(FnExpr expr) {
    return new FnObj(mFnProto, expr.getParamNames(), expr.getBody());
  }

  @Override
  public Obj visit(IfExpr expr) {
    // Evaluate all of the conditions.
    boolean passed = true;
    for (Expr condition : expr.getConditions()) {
      Obj result = evaluate(condition);
      if (!((Boolean)result.getPrimitiveValue()).booleanValue()) {
        // Condition failed.
        passed = false;
        break;
      }
    }
    
    // Evaluate the body.
    if (passed) {
      return evaluate(expr.getThen());
    } else {
      return evaluate(expr.getElse());
    }
  }

  @Override
  public Obj visit(IntExpr expr) {
    return createInt(expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr) {
    boolean done = false;
    while (true) {
      // Evaluate the conditions.
      for (Expr conditionExpr : expr.getConditions()) {
        // See if the while clause is still true.
        Obj condition = evaluate(conditionExpr);
        if (((Boolean)condition.getPrimitiveValue()).booleanValue() != true) {
          done = true;
          break;
        }
      }
      
      // If any clause failed, stop the loop.
      if (done) break;
      
      evaluate(expr.getBody());
    }
    
    // TODO(bob): It would be cool if loops could have "else" clauses and then
    // reliably return a value.
    return nothing();
  }

  @Override
  public Obj visit(MethodExpr expr) {
    Obj receiver = evaluate(expr.getReceiver());
    Obj arg = evaluate(expr.getArg());
    
    return invokeMethod(expr.getMethod(), receiver, arg);
  }

  @Override
  public Obj visit(NameExpr expr) {
    // Look up a named variable.
    Obj variable = mScope.get(expr.getName());
    if (variable == null) {
      throw new InterpreterException("Could not find a variable named \"" + expr.getName() + "\".");
    }
      
    return variable;
  }

  @Override
  public Obj visit(NothingExpr expr) {
    return mNothing;
  }

  @Override
  public Obj visit(StringExpr expr) {
    return createString(expr.getValue());
  }

  @Override
  public Obj visit(TupleExpr expr) {
    // A tuple is an object with fields whose names are zero-based numbers.
    
    Obj tuple = mTupleProto.spawn();
    for (int i = 0; i < expr.getFields().size(); i++) {
      tuple.add(Integer.toString(i), evaluate(expr.getFields().get(i)));
    }
    
    return tuple;
  }
  
  private Obj invokeMethod(String name, Obj thisObj, Obj arg) {
    // Look up the method.
    Invokable method = thisObj.getMethod(name);
    if (method != null) return method.invoke(this, thisObj, arg);
    
    // If there is no arg, look for a field.
    if (arg == mNothing) {
      Obj field = thisObj.getField(name);
      if (field != null) return field;
    }

    throw new InterpreterException("Could not find a member \"" + name + "\" on " + thisObj);
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  // TODO(bob): Get rid of this is a member, and instead pass it around as part
  //            of an evaluation context.
  private Scope mScope;
  private final Obj mRoot;
  private final Obj mNothing;
  private final Obj mBoolProto;
  private final Obj mClassProto;
  private final Obj mFnProto;
  private final Obj mIntProto;
  private final Obj mStringProto;
  private final Obj mTupleProto;
}
