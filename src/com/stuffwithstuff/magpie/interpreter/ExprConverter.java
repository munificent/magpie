package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;

/**
 * Takes an expression in Java form and creates a first-class Magpie Expr object
 * out of it for interacting with code at runtime.
 */
public class ExprConverter implements ExprVisitor<Obj, Void> {
  public static Obj convert(Interpreter interpreter, Expr expr) {
    ExprConverter converter = new ExprConverter(interpreter);
    return converter.convert(expr);
  }
  
  @Override
  public Obj visit(AndExpr expr, Void dummy) {
    Obj left  = convert(expr.getLeft());
    Obj right = convert(expr.getRight());
    return construct("And", left, right);
  }

  @Override
  public Obj visit(ApplyExpr expr, Void dummy) {
    Obj target = convert(expr.getTarget());
    Obj arg = convert(expr.getArg());
    return construct("Apply", target, arg);
  }
  
  @Override
  public Obj visit(AssignExpr expr, Void dummy) {
    Obj receiver;
    if (expr.getReceiver() != null) {
      receiver = convert(expr.getReceiver());
    } else {
      receiver = mInterpreter.nothing();
    }
    
    Obj value = convert(expr.getValue());
    return construct("Assign",
        receiver, mInterpreter.createString(expr.getName()), value);
  }

  @Override
  public Obj visit(BlockExpr expr, Void dummy) {
    List<Obj> exprs = new ArrayList<Obj>();
    for (Expr blockExpr : expr.getExpressions()) {
      exprs.add(convert(blockExpr));
    }
    Obj exprsArray = mInterpreter.createArray(exprs);
    
    return construct("Block", exprsArray);
  }

  @Override
  public Obj visit(BoolExpr expr, Void dummy) {
    return construct("Bool", mInterpreter.createBool(expr.getValue()));
  }

  @Override
  public Obj visit(BreakExpr expr, Void dummy) {
    return construct("Break");
  }

  @Override
  public Obj visit(ExpressionExpr expr, Void dummy) {
    return construct("Expression", convert(expr.getBody()));
  }

  @Override
  public Obj visit(FnExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(IfExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(IntExpr expr, Void dummy) {
    return construct("Int", mInterpreter.createInt(expr.getValue()));
  }

  @Override
  public Obj visit(LoopExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(MessageExpr expr, Void dummy) {
    Obj receiver;
    if (expr.getReceiver() != null) {
      receiver = convert(expr.getReceiver());
    } else {
      receiver = mInterpreter.nothing();
    }
    
    return construct("Message", receiver,
        mInterpreter.createString(expr.getName()));
  }

  @Override
  public Obj visit(NothingExpr expr, Void dummy) {
    return construct("Nothing");
  }

  @Override
  public Obj visit(OrExpr expr, Void dummy) {
    Obj left  = convert(expr.getLeft());
    Obj right = convert(expr.getRight());
    return construct("Or", left, right);
  }

  @Override
  public Obj visit(RecordExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(ReturnExpr expr, Void dummy) {
    return construct("Return", convert(expr.getValue()));
  }

  @Override
  public Obj visit(ScopeExpr expr, Void dummy) {
    return construct("Scope", convert(expr.getBody()));
  }

  @Override
  public Obj visit(StringExpr expr, Void dummy) {
    return construct("String", mInterpreter.createString(expr.getValue()));
  }

  @Override
  public Obj visit(ThisExpr expr, Void dummy) {
    return construct("This");
  }

  @Override
  public Obj visit(TupleExpr expr, Void dummy) {
    List<Obj> fields = new ArrayList<Obj>();
    for (Expr field : expr.getFields()) {
      fields.add(convert(field));
    }
    Obj fieldsArray = mInterpreter.createArray(fields);
    
    return construct("Tuple", fieldsArray);
  }

  @Override
  public Obj visit(TypeofExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(UnsafeCastExpr expr, Void dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(VariableExpr expr, Void dummy) {
    return construct("Variable", mInterpreter.createString(expr.getName()),
        convert(expr.getValue()));
  }
  
  private ExprConverter(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  private Obj convert(Expr expr) {
    return expr.accept(this, null);
  }
  
  private Obj construct(String className, Obj... args) {
    Obj classObj = mInterpreter.getGlobal(className + "Expression");
    
    Obj arg;
    if (args.length == 0) {
      arg = mInterpreter.nothing();
    } else if (args.length == 1) {
      arg = args[0];
    } else {
      arg = mInterpreter.createTuple(args);
    }
    
    return mInterpreter.invokeMethod(classObj, "new", arg);
  }

  private final Interpreter mInterpreter;
}
