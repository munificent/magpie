package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Takes an expression and replaces any special form calls with their canonical
 * Expr classes. Special forms look like regular named function calls, like
 * this:
 * 
 *     %var%("foo", 1 + 2)
 * 
 * The above is equivalent to:
 * 
 *     var foo = 1 + 2
 * 
 * They exist to let you access all Magpie AST nodes using just the core 
 * message syntax before any keyword parsers have been created. The special
 * forms are:
 * 
 * %break%()
 *     A "break" expression.
 * 
 * %return%(value)
 *     A "return" expression. The argument is the returned value.
 *     
 * %var%("name", value)
 *     Defines a local variable with the given name and initial value.  The
 *     first argument must be a string literal containing the name of the
 *     variable. The second argument is the initializer.
 *     
 */
public class ExprSpecialFormDesugarer implements ExprVisitor<Expr, Void> {
  public static Expr desugar(Expr expr) {
    ExprSpecialFormDesugarer desugarer = new ExprSpecialFormDesugarer();
    return desugarer.desugarExpr(expr);
  }
  
  @Override
  public Expr visit(AndExpr expr, Void dummy) {
    Expr left  = desugarExpr(expr.getLeft());
    Expr right = desugarExpr(expr.getRight());
    return new AndExpr(expr.getPosition(), left, right);
  }

  @Override
  public Expr visit(ApplyExpr expr, Void dummy) {
    Expr form = handleSpecialForm(expr);
    if (form != null) return form;
    
    Expr target = desugarExpr(expr.getTarget());
    Expr arg = desugarExpr(expr.getArg());
    
    return new ApplyExpr(target, arg, expr.isStatic());
  }
  
  @Override
  public Expr visit(AssignExpr expr, Void dummy) {
    Expr receiver = desugarExpr(expr.getReceiver());
    Expr value = desugarExpr(expr.getValue());
    return new AssignExpr(expr.getPosition(), receiver, expr.getName(), value);
  }

  @Override
  public Expr visit(BlockExpr expr, Void dummy) {
    List<Expr> exprs = new ArrayList<Expr>();
    for (Expr blockExpr : expr.getExpressions()) {
      exprs.add(desugarExpr(blockExpr));
    }
    Expr catchExpr = desugarExpr(expr.getCatch());
    
    return new BlockExpr(expr.getPosition(), exprs, catchExpr);
  }

  @Override
  public Expr visit(BoolExpr expr, Void dummy) {
    return expr;
  }

  @Override
  public Expr visit(BreakExpr expr, Void dummy) {
    return expr;
  }

  @Override
  public Expr visit(FnExpr expr, Void dummy) {
    Expr paramType = desugarExpr(expr.getType().getParamType());
    Expr returnType = desugarExpr(expr.getType().getReturnType());
    FunctionType type = new FunctionType(expr.getType().getParamNames(),
        paramType, returnType, expr.getType().isStatic());
    Expr body = desugarExpr(expr.getBody());
    return new FnExpr(expr.getPosition(), type, body);
  }

  @Override
  public Expr visit(IfExpr expr, Void dummy) {
    Expr condition = desugarExpr(expr.getCondition());
    Expr thenArm = desugarExpr(expr.getThen());
    Expr elseArm = desugarExpr(expr.getElse());
    return new IfExpr(expr.getPosition(), expr.getName(), condition,
        thenArm, elseArm);
  }

  @Override
  public Expr visit(IntExpr expr, Void dummy) {
    return expr;
  }

  @Override
  public Expr visit(LoopExpr expr, Void dummy) {
    List<Expr> conditions = new ArrayList<Expr>();
    for (Expr condition : expr.getConditions()) {
      conditions.add(desugarExpr(condition));
    }
    Expr body = desugarExpr(expr.getBody());
    
    return new LoopExpr(expr.getPosition(), conditions, body);
  }

  @Override
  public Expr visit(MessageExpr expr, Void dummy) {
    Expr receiver = desugarExpr(expr.getReceiver());
    return new MessageExpr(expr.getPosition(), receiver, expr.getName());
  }

  @Override
  public Expr visit(NothingExpr expr, Void dummy) {
    return expr;
  }

  @Override
  public Expr visit(OrExpr expr, Void dummy) {
    Expr left  = desugarExpr(expr.getLeft());
    Expr right = desugarExpr(expr.getRight());
    return new OrExpr(expr.getPosition(), left, right);
  }

  @Override
  public Expr visit(QuotationExpr expr, Void dummy) {
    Expr body = desugarExpr(expr.getBody());
    return new QuotationExpr(expr.getPosition(), body);
  }

  @Override
  public Expr visit(RecordExpr expr, Void dummy) {
    List<Pair<String, Expr>> fields = new ArrayList<Pair<String, Expr>>();
    for (Pair<String, Expr> field : expr.getFields()) {
      Expr value = desugarExpr(field.getValue());
      fields.add(new Pair<String, Expr>(field.getKey(), value));
    }
    
    return new RecordExpr(expr.getPosition(), fields);
  }

  @Override
  public Expr visit(ReturnExpr expr, Void dummy) {
    Expr value = desugarExpr(expr.getValue());
    return new ReturnExpr(expr.getPosition(), value);
  }

  @Override
  public Expr visit(ScopeExpr expr, Void dummy) {
    Expr body = desugarExpr(expr.getBody());
    return new ScopeExpr(body);
  }

  @Override
  public Expr visit(StringExpr expr, Void dummy) {
    return expr;
  }

  @Override
  public Expr visit(ThisExpr expr, Void dummy) {
    return expr;
  }

  @Override
  public Expr visit(TupleExpr expr, Void dummy) {
    Expr[] fields = new Expr[expr.getFields().size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = desugarExpr(expr.getFields().get(i));
    }
    return new TupleExpr(fields);
  }

  @Override
  public Expr visit(TypeofExpr expr, Void dummy) {
    Expr body = desugarExpr(expr.getBody());
    return new TypeofExpr(expr.getPosition(), body);
  }

  @Override
  public Expr visit(UnquoteExpr expr, Void dummy) {
    Expr body = desugarExpr(expr.getBody());
    return new UnquoteExpr(expr.getPosition(), body);
  }

  @Override
  public Expr visit(UnsafeCastExpr expr, Void dummy) {
    Expr type = desugarExpr(expr.getType());
    Expr value = desugarExpr(expr.getValue());
    return new UnsafeCastExpr(expr.getPosition(), type, value);
  }

  @Override
  public Expr visit(VariableExpr expr, Void dummy) {
    Expr value = desugarExpr(expr.getValue());
    return new VariableExpr(expr.getPosition(), expr.getName(), value);
  }
  
  private ExprSpecialFormDesugarer() {
  }
  
  private Expr desugarExpr(Expr expr) {
    if (expr == null) return null;
    return expr.accept(this, null);
  }
  
  private Expr handleSpecialForm(ApplyExpr expr) {
    // Target must be a simple named message.
    if (!(expr.getTarget() instanceof MessageExpr)) return null;
    MessageExpr message = (MessageExpr) expr.getTarget();
    
    if (message.getReceiver() != null) return null;
    
    String name = message.getName();
    if (name.equals("%break%")) {
      return new BreakExpr(expr.getPosition());
    } else if (name.equals("%return%")) {
      return new ReturnExpr(expr.getPosition(), expr.getArg());
    } else if (name.equals("%unsafecast%")) {
      return specialFormUnsafeCast(expr.getPosition(), expr.getArg());
    } else if (name.equals("%var%")) {
      return specialFormVar(expr.getPosition(), expr.getArg());
    }
    
    return null;
  }
  
  Expr specialFormUnsafeCast(Position position, Expr arg) {
    Expr type = ((TupleExpr)arg).getFields().get(0);
    Expr value = ((TupleExpr)arg).getFields().get(1);
    
    return new UnsafeCastExpr(position, type, value);
  }
  
  Expr specialFormVar(Position position, Expr arg) {
    String name = ((StringExpr)(((TupleExpr)arg).getFields().get(0))).getValue();
    Expr valueExpr = ((TupleExpr)arg).getFields().get(1);
    
    return new VariableExpr(position, name, valueExpr);
  }
}
