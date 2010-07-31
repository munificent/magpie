package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj> {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    // Create a top-level scope.
    mGlobalScope = new Scope();
    mScope = mGlobalScope;
    
    mMetaclass = new ClassObj();
    mScope.define("Class", mMetaclass);
    mMetaclass.getInstanceMethods().put("addInstanceField", ClassMethods.addInstanceField());
    mMetaclass.getInstanceMethods().put("instanceField?", ClassMethods.instanceFieldQ());
    mMetaclass.getInstanceMethods().put("name", ClassMethods.getName());
    mMetaclass.getInstanceMethods().put("new", ClassMethods.construct());
    
    // Register the built-in types.
    createClass("Nothing");
    
    ClassObj boolClass = createClass("Bool");
    boolClass.getInstanceMethods().put("toString", BoolMethods.toStringMethod());
    
    ClassObj intClass = createClass("Int");
    intClass.getInstanceMethods().put("+", IntMethods.operatorPlus());
    intClass.getInstanceMethods().put("-", IntMethods.operatorMinus());
    intClass.getInstanceMethods().put("*", IntMethods.operatorMultiply());
    intClass.getInstanceMethods().put("/", IntMethods.operatorDivide());
    intClass.getInstanceMethods().put("toString", IntMethods.toStringMethod());
    intClass.getInstanceMethods().put("==", IntMethods.operatorEqual());
    intClass.getInstanceMethods().put("!=", IntMethods.operatorNotEqual());
    intClass.getInstanceMethods().put("<",  IntMethods.operatorLessThan());
    intClass.getInstanceMethods().put(">",  IntMethods.operatorGreaterThan());
    intClass.getInstanceMethods().put("<=", IntMethods.operatorLessThanOrEqual());
    intClass.getInstanceMethods().put(">=", IntMethods.operatorGreaterThanOrEqual());
    
    ClassObj stringClass = createClass("String");
    stringClass.getInstanceMethods().put("+",     StringMethods.operatorPlus());
    stringClass.getInstanceMethods().put("print", StringMethods.print());
    
    ClassObj fnClass = createClass("Function");
    fnClass.getInstanceMethods().put("invoke", FnMethods.invoke());

    // Register the () object.
    mNothing = new Obj(findClass("Nothing"), null);
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
    return new Obj(findClass("Bool"), value);
  }

  public Obj createInt(int value) {
    return new Obj(findClass("Int"), value);
  }
  
  public Obj createString(String text) {
    return new Obj(findClass("String"), text);
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
      TupleObj tuple = (TupleObj)arg;
      for (int i = 0; i < paramNames.size(); i++) {
        mScope.define(paramNames.get(i), tuple.getFields().get(i));
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
    return new Obj(findClass("Bool"), expr.getValue());
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
    ClassObj classObj = new ClassObj(mMetaclass, expr.getName());
    
    // Define the fields.
    for (String field : expr.getFields().keySet()) {
      classObj.getInstanceFields().put(field, true);
    }
    
    // Define the methods.
    for (String name : expr.getMethods().keySet()) {
      // TODO(bob): Is there a better way to iterate over a map?
      Method method = new Method(expr.getMethods().get(name));
      classObj.getInstanceMethods().put(name, method);
    }
    
    // TODO(bob): Hack. Temp.
    
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
    return new FnObj(findClass("Function"), expr.getParamNames(), expr.getBody());
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
    return new Obj(findClass("Int"), expr.getValue());
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
    List<Obj> fields = new ArrayList<Obj>();
    
    for (Expr field : expr.getFields()) {
      fields.add(evaluate(field));
    }
    
    return new TupleObj(fields);
  }
  
  private Obj invokeMethod(String name, Obj thisObj, Obj arg) {
    // See if the object itself has the method.
    Invokable method = thisObj.getMethods().get(name);
    
    // If not, see if it's type has an instance method for it.
    if (method == null) {
      ClassObj thisClass = thisObj.getClassObj();
      method = thisClass.getInstanceMethods().get(name);
    }
    
    if (method == null) {
      throw new InterpreterException("Could not find a method \"" + name + "\" on " + thisObj);
    }
    
    return method.invoke(this, thisObj, arg);
  }
  
  private ClassObj createClass(String name) {
    ClassObj classObj = new ClassObj(mMetaclass, name);
    mScope.define(name, classObj);
    return classObj;
  }
  
  private ClassObj findClass(String name) {
    return (ClassObj)mGlobalScope.get(name);
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  // TODO(bob): Get rid of this is a member, and instead pass it around as part
  //            of an evaluation context.
  private Scope mScope;
  private final ClassObj mMetaclass;
  private final Obj mNothing;
}
