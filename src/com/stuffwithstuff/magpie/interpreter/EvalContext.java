package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope and the object that "this" refers to.
 */
public class EvalContext {
  public EvalContext(Scope scope, Obj thisObj, ClassObj containingClass) {
    mScope = scope;
    mThis = thisObj;
    mContainingClass = containingClass;
    mIsInLoop = false;
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   */
  public EvalContext pushScope() {
    return new EvalContext(new Scope(mScope), mThis, mContainingClass, mIsInLoop);
  }
  
  /**
   * Creates an EvalContext that discards the current innermost lexical scope.
   */
  public EvalContext popScope() {
    return new EvalContext(mScope.getParent(), mThis, mContainingClass, mIsInLoop);
  }

  /**
   * Creates a new EvalContext with the same scope as this one, but inside a
   * loop.
   */
  public EvalContext enterLoop() {
    return new EvalContext(mScope, mThis, mContainingClass, true);
  }
  
  public Scope   getScope() { return mScope; }
  public ClassObj getContainingClass() { return mContainingClass; }

  public Obj getThis() {
    // TODO(bob): Temp work-in-progress. Right now, multimethods bind the
    // receiver to "this_". Once they are used for everything, the special
    // "this" AST node can go away completely, and they can just bind to "this".
    Obj tempThis = lookUp("this_");
    if (tempThis != null) return tempThis;
    
    return mThis;
  }
  
  public boolean isInLoop() { return mIsInLoop; }
  
  /**
   * Looks up the given name in the context's lexical scope chain.
   * @param   name The name of the variable to look up.
   * @return       The value bound to that name, or null if not found.
   */
  public Obj lookUp(String name) {
    if (!name.contains(".")) {
      // An unqualified name, so walk the used namespaces first.
      for (String namespace : mScope.getNamespaces()) {
        Obj object = lookUpName(namespace + "." + name);
        if (object != null) return object;
      }
    }
    
    // If we got here, it was already qualified, or wasn't in any namespace, so
    // try the global one.
    return lookUpName(name);
  }

  /**
   * Defines a new variable with the given name in this context's current scope.
   * 
   * @param name  Name of the variable to define.
   * @param value The variable's value.
   * @return      True if it was a new name and was defined, false if there is
   *              already a variable with that name in this scope and no new
   *              definition was created.
   */
  public boolean define(String name, Obj value) {
    if (!mScope.canDefine(name)) return false;
    
    mScope.define(name, value);
    return true;
  }
  
  /**
   * Assigns the given value to an existing variable with the given name in the
   * scope where the name is already defined. Does nothing if there is no
   * variable with that name. Note this is different from {code define}, which
   * always binds in the current lexical scope and would shadow an existing
   * definition in an outer scope. This will walk up the scope chain to assign
   * the value wherever it's already defined.
   * 
   * @param name  Name of the variable to assign.
   * @param value The variable's value.
   * @return      True if the variable was found, otherwise false.
   */
  public boolean assign(String name, Obj value) {
    Scope scope = mScope;
    while (scope != null) {
      if (scope.assign(name, value)) return true;
      scope = scope.getParent();
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("this -> ").append(mThis).append("\n");
    
    String indent = "";
    Scope scope = mScope;
    while (scope != null) {
      for (Entry<String, Obj> entry : scope.entries()) {
        builder.append(indent).append(entry.getKey())
               .append(" -> ").append(entry.getValue()).append("\n");
      }
      
      indent = indent + "  ";
      scope = scope.getParent();
    }
    
    return builder.toString();
  }
  
  private Obj lookUpName(String name) {
    // Walk up the scope chain until we find it.
    Scope scope = mScope;
    while (scope != null) {
      Obj value = scope.get(name);
      if (value != null) return value;
      scope = scope.getParent();
    }
    
    return null;
  }
  
  private EvalContext(Scope scope, Obj thisObj, ClassObj containingClass, boolean isInLoop) {
    mScope = scope;
    mThis = thisObj;
    mContainingClass = containingClass;
    mIsInLoop = isInLoop;
  }

  private final Scope   mScope;
  private final Obj     mThis;
  private final ClassObj mContainingClass;
  private final boolean mIsInLoop;
}
