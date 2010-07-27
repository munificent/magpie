package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj> {
  public Interpreter() {
    mTypeType = new TypeObj(mNextTypeId++);
    mScope.put("Type", mTypeType);

    // Register the built-in types.
    createType("Unit");
    createType("Bool");
    
    TypeObj intType = createType("Int");
    intType.addMethod("+", IntMethods.operatorPlus());
    intType.addMethod("-", IntMethods.operatorMinus());
    
    TypeObj stringType = createType("String");
    stringType.addMethod("+",     StringMethods.operatorPlus());
    stringType.addMethod("print", StringMethods.print());
    
    // Register the () object.
    mUnit = new Obj((TypeObj)find("Unit"), null);
  }
  
  public Obj evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  /**
   * Gets the single value () of type Unit.
   * @return
   */
  public Obj unit() { return mUnit; }

  @Override
  public Obj visit(BlockExpr expr) {
    Obj result = null;
    
    // TODO(bob): Need to create a local scope.
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr) {
    return new Obj((TypeObj)find("Bool"), expr.getValue());
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
    
    // If the target is a name, try to call it as a method on the argument.
    // In other words "abs 123" is equivalent to "123.abs".
    if (expr.getTarget() instanceof NameExpr) {
      NameExpr targetName = (NameExpr)expr.getTarget();
      TypeObj argType = arg.getType();
      Method method = argType.getMethod(targetName.getName(), argType);
      return method.invoke(this, arg, unit());
    }
    
    throw new Error("Couldn't interpret call.");
  }

  @Override
  public Obj visit(IntExpr expr) {
    return new Obj((TypeObj)find("Int"), expr.getValue());
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
    return mScope.get(expr.getName());
  }

  @Override
  public Obj visit(StringExpr expr) {
    return new Obj((TypeObj)find("String"), expr.getValue());
  }

  @Override
  public Obj visit(TupleExpr expr) {
    // TODO(bob): Fill this in once we have tuple objects.
    return null;
  }

  @Override
  public Obj visit(UnitExpr expr) {
    return mUnit;
  }
  
  private TypeObj createType(String name) {
    TypeObj typeObj = new TypeObj(mTypeType, mNextTypeId++, name);
    mScope.put(name, typeObj);
    return typeObj;
  }
  
  private Obj find(String name) {
    return mScope.get(name);
  }
  
  private final Map<String, Obj> mScope = new HashMap<String, Obj>();
  private final TypeObj mTypeType;
  private final Obj mUnit;
  private int mNextTypeId = 0;
}
