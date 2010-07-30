package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj> {
  public Interpreter(InterpreterHost host, SourceFile sourceFile) {
    mHost = host;
    
    // Build a map of the defined functions.
    for (FunctionDefn function : sourceFile.getFunctions()) {
      mFunctions.put(function.getName(), function);
    }
    
    // Create a top-level scope.
    mScope = new Scope();

    mMetaclass = new ClassObj();
    mScope.put("Class", mMetaclass);
    mMetaclass.addInstanceMethod("addInstanceField", ClassMethods.addInstanceField());
    mMetaclass.addInstanceMethod("instanceField?", ClassMethods.instanceFieldQ());
    mMetaclass.addInstanceMethod("name", ClassMethods.name());
    
    // Register the built-in types.
    createClass("Nothing");
    
    ClassObj boolClass = createClass("Bool");
    boolClass.addInstanceMethod("toString", BoolMethods.toStringMethod());
    
    ClassObj intClass = createClass("Int");
    intClass.addInstanceMethod("+", IntMethods.operatorPlus());
    intClass.addInstanceMethod("-", IntMethods.operatorMinus());
    intClass.addInstanceMethod("*", IntMethods.operatorMultiply());
    intClass.addInstanceMethod("/", IntMethods.operatorDivide());
    intClass.addInstanceMethod("toString", IntMethods.toStringMethod());
    
    ClassObj stringClass = createClass("String");
    stringClass.addInstanceMethod("+",     StringMethods.operatorPlus());
    stringClass.addInstanceMethod("print", StringMethods.print());
    
    // Register the () object.
    mNothing = new Obj(findClass("Nothing"), null);
  }
  
  public Obj evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  public Obj runMain() {
    FunctionDefn main = mFunctions.get("main");
    if (main == null) throw new InterpreterException("Couldn't find a main method.");
    
    return invoke(main, nothing());
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
  
  @Override
  public Obj visit(AssignExpr expr) {
    Obj value = evaluate(expr.getValue());
    mScope.put(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(BlockExpr expr) {
    Obj result = null;
    
    // Create a lexical scope.
    mScope.push();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr);
    }
    
    mScope.pop();
    
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
      // TODO(bob): Implement me.
      
      // Look for a defined function with the name.
      FunctionDefn function = mFunctions.get(name);
      if (function != null) {
        return invoke(function, arg);
      }
      
      // Try to call it as a method on the argument. In other words,
      // "abs 123" is equivalent to "123.abs".
      return invokeMethod(name, arg, nothing());
    }
    
    throw new InterpreterException("Couldn't interpret call.");
  }

  @Override
  public Obj visit(ClassExpr expr) {
    ClassObj classObj = new ClassObj(mMetaclass, expr.getName());
    
    for (String field : expr.getFields().keySet()) {
      classObj.addInstanceField(field);
    }
    
    mScope.put(expr.getName(), classObj);
    return classObj;
  }

  @Override
  public Obj visit(DefineExpr expr) {
    // TODO(bob): need to handle mutability
    Obj value = evaluate(expr.getValue());
    mScope.put(expr.getName(), value);
    return value;
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
  public Obj visit(MethodExpr expr) {
    Obj receiver = evaluate(expr.getReceiver());
    Obj arg = evaluate(expr.getArg());
    
    return invokeMethod(expr.getMethod(), receiver, arg);
  }

  @Override
  public Obj visit(NameExpr expr) {
    // See if it's a named variable.
    Obj variable = mScope.get(expr.getName());
    if (variable != null) return variable;
    
    // Not a variable. Must be a call to function with an implicit ().
    FunctionDefn function = mFunctions.get(expr.getName());
    return invoke(function, nothing());
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
    Method method = thisObj.getMethods().get(name);
    
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
  
  private Obj invoke(FunctionDefn function, Obj arg) {
    // Create a new local scope for the function.
    mScope = mScope.push();
    
    // Bind arguments to their parameter names.
    if (function.getParamNames().size() == 1) {
      mScope.put(function.getParamNames().get(0), arg);
    } else if (function.getParamNames().size() > 1) {
      TupleObj tuple = (TupleObj)arg;
      for (int i = 0; i < function.getParamNames().size(); i++) {
        mScope.put(function.getParamNames().get(i), tuple.getFields().get(i));
      }
    }
    
    Obj result = evaluate(function.getBody());
    
    // Restore the previous scope.
    mScope = mScope.pop();
    
    return result;
  }
  
  private ClassObj createClass(String name) {
    ClassObj classObj = new ClassObj(mMetaclass, name);
    mScope.put(name, classObj);
    return classObj;
  }
  
  private ClassObj findClass(String name) {
    return (ClassObj)mScope.get(name);
  }
  
  private final InterpreterHost mHost;
  private final Map<String, FunctionDefn> mFunctions =
      new HashMap<String, FunctionDefn>();
  private Scope mScope;
  private final ClassObj mMetaclass;
  private final Obj mNothing;
}
