package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import com.stuffwithstuff.magpie.ast.*;

public class Interpreter implements ExprVisitor<Obj> {
  public Interpreter() {
    // register the built-in types
    TypeObj typeObject = new TypeObj();
    mScope.put("Type", typeObject);
    mScope.put("Unit", new TypeObj(typeObject, "Unit"));
    mScope.put("Bool", new TypeObj(typeObject, "Bool"));
    
    TypeObj intType = new TypeObj(typeObject, "Int");
    mScope.put("Int", intType);
    intType.addMethod("+",     IntMethods.operatorPlus());
    intType.addMethod("-",     IntMethods.operatorMinus());
  }
  
  public Obj evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Obj visit(BlockExpr expr) {
    Obj result = null;
    
    // evaluate all of the expressions and return the last
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
    // TODO(bob): Fill this in once we have function objects.
    return null;
  }

  @Override
  public Obj visit(IntExpr expr) {
    return new Obj((TypeObj)find("Int"), expr.getValue());
  }

  @Override
  public Obj visit(MethodExpr expr) {
    Obj receiver = evaluate(expr.getReceiver());
    Obj arg = evaluate(expr.getArg());
    
    // TODO(bob): getMethod also needs to take in the argument type so that it
    //            can select the appropriate overloaded method.
    Method method = receiver.getType().getMethod(expr.getMethod());
    return method.invoke(this, receiver, arg);
  }

  @Override
  public Obj visit(NameExpr expr) {
    return mScope.get(expr.getName());
  }

  @Override
  public Obj visit(TupleExpr expr) {
    // TODO(bob): Fill this in once we have tuple objects.
    return null;
  }

  @Override
  public Obj visit(UnitExpr expr) {
    return new Obj((TypeObj)find("Unit"));
  }
  
  private Obj find(String name) {
    return mScope.get(name);
  }
  
  private final Map<String, Obj> mScope = new HashMap<String, Obj>();
}
