package com.stuffwithstuff.magpie.ast;

import com.stuffwithstuff.magpie.parser.Position;

/**
 * AST node for a function application: applies an argument to a function-like
 * target.
 */
public class ApplyExpr extends Expr {
  public static Expr create(Expr target, Expr arg, boolean isStatic) {
    // Immediately handle special forms.
    if (target instanceof MessageExpr) {
      MessageExpr message = (MessageExpr) target;
      String name = message.getName();
      
      if (name.equals("%break%")) {
        return new BreakExpr(target.getPosition());
      } else if (name.equals("%return%")) {
        return new ReturnExpr(target.getPosition(), arg);
      } else if (name.equals("%unsafecast%")) {
        return specialFormUnsafeCast(target.getPosition(), arg);
      } else if (name.equals("%var%")) {
        return specialFormVar(target.getPosition(), arg);
      }
    }
    
    // If we got here, it's not a special form.
    return new ApplyExpr(target, arg, isStatic);
  }
  
  private static Expr specialFormUnsafeCast(Position position, Expr arg) {
    Expr type = ((TupleExpr)arg).getFields().get(0);
    Expr value = ((TupleExpr)arg).getFields().get(1);
    
    return new UnsafeCastExpr(position, type, value);
  }
  
  private static Expr specialFormVar(Position position, Expr arg) {
    String name = ((StringExpr)(((TupleExpr)arg).getFields().get(0))).getValue();
    Expr valueExpr = ((TupleExpr)arg).getFields().get(1);
    
    return new VariableExpr(position, name, valueExpr);
  }
  
  private ApplyExpr(Expr target, Expr arg, boolean isStatic) {
    super(target.getPosition().union(arg.getPosition()));
    mTarget = target;
    mArg = arg;
    mIsStatic = isStatic;
  }
  
  public Expr getTarget() { return mTarget; }
  public Expr getArg() { return mArg; }
  public boolean isStatic() { return mIsStatic; }
  
  @Override
  public <R, C> R accept(ExprVisitor<R, C> visitor, C context) {
    return visitor.visit(this, context);
  }

  @Override
  public void toString(StringBuilder builder, String indent) {
    mTarget.toString(builder, indent);
    builder.append(mIsStatic ? "[" : "(");
    mArg.toString(builder, indent);
    builder.append(mIsStatic ? "]" : ")");
  }

  private final Expr mTarget;
  private final Expr mArg;
  private final boolean mIsStatic;
}
