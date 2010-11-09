package com.stuffwithstuff.magpie.interpreter;

import java.util.List;
import java.util.Map.Entry;

/**
 * Describes the context in which an expression can be evaluated. Includes the
 * lexical scope and the object that "this" refers to.
 */
public class EvalContext {
  public EvalContext(Scope scope, Obj thisObj) {
    mScope = scope;
    mThis = thisObj;
    mIsInLoop = false;
  }
  
  /**
   * Creates an EvalContext for a new lexical block scope within this one.
   */
  public EvalContext pushScope() {
    return new EvalContext(new Scope(mScope), mThis, mIsInLoop);
  }
  
  /**
   * Creates an EvalContext that discards the current innermost lexical scope.
   */
  public EvalContext popScope() {
    return new EvalContext(mScope.getParent(), mThis, mIsInLoop);
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but bound to a
   * different this reference.
   */
  public EvalContext withThis(Obj thisObj) {
    return new EvalContext(mScope, thisObj, mIsInLoop);
  }
  
  /**
   * Creates a new EvalContext with the same scope as this one, but inside a
   * loop.
   */
  public EvalContext enterLoop() {
    return new EvalContext(mScope, mThis, true);
  }
  
  public Scope   getScope() { return mScope; }
  public Obj     getThis()  { return mThis; }
  public boolean isInLoop() { return mIsInLoop; }
  
  /**
   * Looks up the given name in current local scope. Does not walk up the
   * lexical scope chain.
   * @param   name The name of the variable to look up.
   * @return       The value bound to that name, or null if not found.
   */
  public Obj lookUpHere(String name) {
    return mScope.get(name);
  }
  
  /**
   * Looks up the given name in the context's lexical scope chain.
   * @param   name The name of the variable to look up.
   * @return       The value bound to that name, or null if not found.
   */
  public Obj lookUp(String name) {
    // Walk up the scope chain until we find it.
    Scope scope = mScope;
    while (scope != null) {
      Obj value = scope.get(name);
      if (value != null) return value;
      scope = scope.getParent();
    }
    
    return null;
  }

  /**
   * Defines (or redefines) a variable with the given name in this context's
   * current scope.
   * 
   * @param name  Name of the variable to define.
   * @param value The variable's value.
   */
  public void define(String name, Obj value) {
    mScope.define(name, value);
  }
  
  /**
   * Defines a list of names by decomposing the given value tuple if needed.
   * @param names  The names of the parameters to bind.
   * @param value  The value to bind the names to.
   */
  public void bind(Interpreter interpreter, List<String> names, Obj value) {
    if (names.size() == 1) {
      define(names.get(0), value);
    } else if (names.size() > 1) {
      // Make sure the argument's structure matches our expected parameter list.
      // If it doesn't, ignore extra tuple fields and pad missing ones with
      // nothing.
      if (value.getClassObj() != interpreter.getTupleType()) {
        // Not a tuple and we're expecting it to be, so just bind it to the
        // first parameter and define the others as nothing.
        define(names.get(0), value);
        
        for (int i = 1; i < names.size(); i++) {
          define(names.get(i), interpreter.nothing());
        }
      } else {
        // Destructure the tuple.
        for (int i = 0; i < names.size(); i++) {
          Obj field = value.getTupleField(i);
          if (field == null) field = interpreter.nothing();
          define(names.get(i), field);
        }
      }
    }
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
  
  private EvalContext(Scope scope, Obj thisObj, boolean isInLoop) {
    mScope = scope;
    mThis = thisObj;
    mIsInLoop = isInLoop;
  }
  
  private final Scope   mScope;
  private final Obj     mThis;
  private final boolean mIsInLoop;
}
