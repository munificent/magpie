package com.stuffwithstuff.magpie.interpreter;

import java.util.HashSet;
import java.util.Set;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Walks a constructor body to verify that all of the class's fields are
 * definitely assigned by the time the constructor is completed. Also ensures
 * fields are not read before they are assigned.
 */
public class DefiniteFieldAssignment implements ExprVisitor<Set<String>, EvalContext> {
  public DefiniteFieldAssignment() {
  }
  
  public void check(Checker checker, ClassObj classObj) {
    for (String field : classObj.getFieldDeclarations().keySet()) {
      mFields.add(field);
    }
    
    Position position = Position.none();
    Set<String> assigned = null;
    if (classObj.getConstructor() instanceof Function) {
      Expr constructor = ((Function)classObj.getConstructor()).getFunction().getBody();
      position = constructor.getPosition();
      
      assigned = visit(constructor, null);
    }
    
    for (String field : mFields) {
      if (!assigned.contains(field)) {
        checker.addError(position,
            "Field \"%s\" in class %s may not have been assigned.",
            field, classObj.getName());
      }
    }
  }
  
  @Override
  public Set<String> visit(AndExpr expr, EvalContext context) {
    Set<String> left = visit(expr.getLeft(), context);
    Set<String> right = visit(expr.getRight(), context);

    return intersect(left, right);
  }

  @Override
  public Set<String> visit(ApplyExpr expr, EvalContext context) {
    Set<String> target = visit(expr.getTarget(), context);
    Set<String> arg = visit(expr.getArg(), context);
    
    return union(target, arg);
  }
  
  @Override
  public Set<String> visit(AssignExpr expr, EvalContext context) {
    Set<String> value = visit(expr.getValue(), context);
    Set<String> receiver = visit(expr.getReceiver(), context);
    
    Set<String> assigned = union(value, receiver);
    
    if ((expr.getReceiver() == null) ||
        (expr.getReceiver() instanceof ThisExpr)) {
      if (mFields.contains(expr.getName())) {
        assigned.add(expr.getName());
      }
    }
    
    return assigned;
  }

  @Override
  public Set<String> visit(BlockExpr expr, EvalContext context) {
    Set<String> assigned = emptySet();
    
    for (Expr blockExpr : expr.getExpressions()) {
      assigned.addAll(visit(blockExpr, context));
    }
    
    return assigned;
  }

  @Override
  public Set<String> visit(BoolExpr expr, EvalContext context) {
    return emptySet();
  }

  @Override
  public Set<String> visit(BreakExpr expr, EvalContext context) {
    return emptySet();
  }

  @Override
  public Set<String> visit(ExpressionExpr expr, EvalContext context) {
    return emptySet();
  }
  
  @Override
  public Set<String> visit(FnExpr expr, EvalContext context) {
    // TODO(bob): Need to handle references to a field inside the fn. A fn
    // probably cannot definitely assign (because we don't know when/if it will
    // be invoked) but we still need to check that it doesn't try to read from
    // an unassigned field.
    return emptySet();
  }

  @Override
  public Set<String> visit(IfExpr expr, EvalContext context) {
    Set<String> condition = visit(expr.getCondition(), context);
    Set<String> thenArm = visit(expr.getThen(), context);
    Set<String> elseArm = visit(expr.getElse(), context);

    return union(condition, intersect(thenArm, elseArm));
  }

  @Override
  public Set<String> visit(IntExpr expr, EvalContext context) {
    return emptySet();
  }

  @Override
  public Set<String> visit(LoopExpr expr, EvalContext context) {
    // TODO(bob): Because loops don't have an 'else' arm, they can't help with
    // definite assignment yet.
    // TODO(bob): Still need to check that fields aren't read inside loop body.
    return emptySet();
  }

  @Override
  public Set<String> visit(MessageExpr expr, EvalContext context) {
    Set<String> receiver = visit(expr.getReceiver(), context);
    
    // TODO(bob): Make sure we aren't reading from an unassigned field.
    return receiver;
  }
  
  @Override
  public Set<String> visit(NothingExpr expr, EvalContext context) {
    return emptySet();
  }
  
  @Override
  public Set<String> visit(ObjectExpr expr, EvalContext context) {
    Set<String> assigned = emptySet();
    
    for (Pair<String, Expr> field : expr.getFields()) {
      assigned.addAll(visit(field.getValue(), context));
    }
    
    return assigned;
  }

  @Override
  public Set<String> visit(OrExpr expr, EvalContext context) {
    Set<String> left = visit(expr.getLeft(), context);
    Set<String> right = visit(expr.getRight(), context);

    return intersect(left, right);
  }

  @Override
  public Set<String> visit(ReturnExpr expr, EvalContext context) {
    // TODO(bob): What about reachability?
    return emptySet();
  }
  
  @Override
  public Set<String> visit(ScopeExpr expr, EvalContext context) {
    return visit(expr.getBody(), context);
  }
  
  @Override
  public Set<String> visit(StringExpr expr, EvalContext context) {
    return emptySet();
  }

  @Override
  public Set<String> visit(ThisExpr expr, EvalContext context) {
    return emptySet();
  }

  @Override
  public Set<String> visit(TypeofExpr expr, EvalContext context) {
    return emptySet();
  }

  @Override
  public Set<String> visit(TupleExpr expr, EvalContext context) {
    Set<String> assigned = emptySet();
    for (Expr field : expr.getFields()) {
      assigned.addAll(visit(field, context));
    }
    
    return assigned;
  }

  @Override
  public Set<String> visit(UnsafeCastExpr expr, EvalContext context) {
    Set<String> type = visit(expr.getType(), context);
    Set<String> value = visit(expr.getValue(), context);
    
    return union(type, value);
  }

  @Override
  public Set<String> visit(VariableExpr expr, EvalContext context) {
    Set<String> value = visit(expr.getValue(), context);
    
    // TODO(bob): Need to handle variables that shadow fields.
    return value;
  }

  private Set<String> emptySet() {
    return new HashSet<String>();
  }

  private Set<String> union(Set<String> left, Set<String> right) {
    Set<String> result = new HashSet<String>(left);
    result.addAll(right);
    return result;
  }

  private Set<String> intersect(Set<String> left, Set<String> right) {
    Set<String> result = new HashSet<String>(left);
    result.retainAll(right);
    return result;
  }
  
  private Set<String> visit(Expr expr, EvalContext context) {
    if (expr == null) return emptySet();
    return expr.accept(this, context);
  }
  
  private final Set<String> mFields = new HashSet<String>();
}