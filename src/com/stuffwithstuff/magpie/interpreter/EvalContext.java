package com.stuffwithstuff.magpie.interpreter;

// TODO(bob): Once we can detect loops statically, this class can probably just
// go away completely.
/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope and the object that "this" refers to.
 */
public class EvalContext {
  public EvalContext(Scope scope) {
    mScope = scope;
    mIsInLoop = false;
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   */
  public EvalContext pushScope() {
    return new EvalContext(new Scope(mScope), mIsInLoop);
  }
  
  /**
   * Creates an EvalContext that discards the current innermost lexical scope.
   */
  public EvalContext popScope() {
    return new EvalContext(mScope.getParent(), mIsInLoop);
  }

  /**
   * Creates a new EvalContext with the same scope as this one, but inside a
   * loop.
   */
  public EvalContext enterLoop() {
    return new EvalContext(mScope, true);
  }
  
  public Scope   getScope() { return mScope; }
  public boolean isInLoop() { return mIsInLoop; }
  
  private EvalContext(Scope scope, boolean isInLoop) {
    mScope = scope;
    mIsInLoop = isInLoop;
  }

  private final Scope    mScope;
  private final boolean  mIsInLoop;
}
