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
    
    mTypeType = new TypeObj();
    mTypes.put("Type", mTypeType);
    
    // Register the built-in types.
    createType("Unit");
    createType("Bool");
    
    TypeObj intType = createType("Int");
    intType.addMethod("+", IntMethods.operatorPlus());
    intType.addMethod("-", IntMethods.operatorMinus());
    intType.addMethod("*", IntMethods.operatorMultiply());
    intType.addMethod("/", IntMethods.operatorDivide());
    intType.addMethod("toString", IntMethods.toStringMethod());
    
    TypeObj stringType = createType("String");
    stringType.addMethod("+",     StringMethods.operatorPlus());
    stringType.addMethod("print", StringMethods.print());
    
    // Register the () object.
    mUnit = new Obj(findType("Unit"), null);
    
    // Create a top-level scope.
    mScope = new Scope();
  }
  
  public Obj evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  public Obj runMain() {
    FunctionDefn main = mFunctions.get("main");
    if (main == null) throw new IllegalStateException("Couldn't find a main method.");
    
    return invoke(main, unit());
  }
  
  public void print(String text) {
    mHost.print(text);
  }
  
  public Obj createString(String text) {
    return new Obj(findType("String"), text);
  }
  
  /**
   * Gets the single value () of type Unit.
   * @return
   */
  public Obj unit() { return mUnit; }

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
    return new Obj(findType("Bool"), expr.getValue());
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
      TypeObj argType = arg.getType();
      Method method = argType.getMethod(name, argType);
      return method.invoke(this, arg, unit());
    }
    
    // TODO(bob): The type checker should prevent this from happening.
    throw new RuntimeException("Couldn't interpret call.");
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
    return new Obj(findType("Int"), expr.getValue());
  }

  @Override
  public Obj visit(MethodExpr expr) {
    Obj receiver = evaluate(expr.getReceiver());
    Obj arg = evaluate(expr.getArg());
    
    Method method = receiver.getType().getMethod(expr.getMethod(),
        arg.getType());
    return method.invoke(this, receiver, arg);
  }

  @Override
  public Obj visit(NameExpr expr) {
    // See if it's a named variable.
    Obj variable = mScope.get(expr.getName());
    if (variable != null) return variable;
    
    // Not a variable. Must be a call to function with an implicit ().
    FunctionDefn function = mFunctions.get(expr.getName());
    return invoke(function, unit());
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

  @Override
  public Obj visit(UnitExpr expr) {
    return mUnit;
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
  
  private TypeObj createType(String name) {
    TypeObj typeObj = new TypeObj(mTypeType, name);
    mTypes.put(name, typeObj);
    return typeObj;
  }
  
  private TypeObj findType(String name) {
    return mTypes.get(name);
  }
  
  private final InterpreterHost mHost;
  private final Map<String, FunctionDefn> mFunctions =
      new HashMap<String, FunctionDefn>();
  private final Map<String, TypeObj> mTypes = new HashMap<String, TypeObj>();
  private Scope mScope;
  private final TypeObj mTypeType;
  private final Obj mUnit;
}
