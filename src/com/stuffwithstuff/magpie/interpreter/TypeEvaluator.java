package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;

public class TypeEvaluator implements ExprVisitor<Obj, EvalContext> {

  public static Obj evaluate(Interpreter interpreter, Expr expr,
      EvalContext context) {
    
    TypeEvaluator evaluator = new TypeEvaluator(interpreter);
    return expr.accept(evaluator, context);
  }
  
  private TypeEvaluator(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  @Override
  public Obj visit(ArrayExpr expr, EvalContext context) {
    // Try to infer the element type from the contents. The rules (which may
    // change over time) are:
    // 1. An empty array has element type Object.
    // 2. If all elements are == to the first, that's the element type.
    // 3. Otherwise, it's Object.
    
    // Other currently unsupported options would be:
    // For classes, look for a common base class.
    // For unrelated types, | them together.

    List<Expr> elements = expr.getElements();
    
    ExprEvaluator evaluator = new ExprEvaluator(mInterpreter);

    Obj elementType;
    if (elements.size() == 0) {
      elementType = mInterpreter.getObjectType();
    } else {
      // Get the first element's type.
      elementType = evaluate(elements.get(0), context);
      
      // Compare all of the others to it, if any.
      if (elements.size() > 1) {
        for (int i = 1; i < elements.size(); i++) {
          Obj other = evaluate(elements.get(i), context);
          Obj result = evaluator.invokeMethod(expr, elementType, "==", other);
          if (!result.asBool()) {
            // No match, so default to Object.
            elementType = mInterpreter.getObjectType();
            break;
          }
        }
      }
    }
    
    Obj arrayType = mInterpreter.getArrayType();
    return evaluator.invokeMethod(expr, arrayType, "newType", elementType);
  }

  @Override
  public Obj visit(AndExpr expr, EvalContext context) {
    Obj left = evaluate(expr.getLeft(), context);
    Obj right = evaluate(expr.getRight(), context);
    
    return orTypes(expr, left, right, mInterpreter.getBoolType());
  }

  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    // An assignment returns the value being assigned.
    return evaluate(expr.getValue(), context);
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    // TODO(bob): This is maybe a little fishy. We're creating a new context
    // that will hold types now and not values. Do we need to explicitly split
    // these up in the interpreter?
    EvalContext localContext = context.newBlockScope();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr, localContext);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.getBoolType();
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    Obj thenType = evaluate(expr.getThen(), context);
    Obj elseType = evaluate(expr.getElse(), context);
    
    return orTypes(expr, thenType, elseType);
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.getIntType();
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    // Right now, loops can't evaluate to anything.
    return mInterpreter.getNothingType();
  }

  @Override
  public Obj visit(MessageExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.getNothingType();
  }
  
  @Override
  public Obj visit(OrExpr expr, EvalContext context) {
    Obj left = evaluate(expr.getLeft(), context);
    Obj right = evaluate(expr.getRight(), context);
    
    return orTypes(expr, left, right, mInterpreter.getBoolType());
  }

  @Override
  public Obj visit(ReturnExpr expr, EvalContext context) {
    return mInterpreter.getNeverType();
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return mInterpreter.getStringType();
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // A tuple type is just a tuple of types.
    List<Obj> fields = new ArrayList<Obj>();
    for (Expr field : expr.getFields()) {
      fields.add(evaluate(field, context));
    }
    
    return mInterpreter.createTuple(fields);
  }

  @Override
  public Obj visit(TypeofExpr expr, EvalContext context) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Obj visit(VariableExpr expr, EvalContext context) {
    // TODO(bob): Needs to bind the name to its type here too.
    
    // Variable definitions return the defined value.
    return evaluate(expr.getValue(), context);
  }
  
  private Obj evaluate(Expr expr, EvalContext context) {
    return expr.accept(this, context);
  }
  
  private Obj orTypes(Expr expr, Obj... types) {
    ExprEvaluator evaluator = new ExprEvaluator(mInterpreter);
    
    Obj result = types[0];
    for (int i = 1; i < types.length; i++) {
      result = evaluator.invokeMethod(expr, result, "|", types[i]);
    }
    
    return result;
  }
  
  private final Interpreter mInterpreter;
}
