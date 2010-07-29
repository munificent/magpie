package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

/**
 * A lexical scope for named variables. Maintains a stack of nested scopes to
 * support proper block scoping.
 */
public class Scope {
  public Scope() {
    // Push an initial empty scope onto the stack.
    push();
  }
  
  public void put(String name, Obj value) {
    mVariables.peek().put(name, value);
  }
  
  public Obj get(String name) {
    // Walk up the scope chain.
    for (Map<String, Obj> scope : mVariables) {
      Obj value = scope.get(name);
      if (value != null) return value;
    }
    
    // If we got here, it wasn't defined.
    throw new Error("Undefined var. Type-checker should catch this.");
  }
  
  public void push() {
    mVariables.push(new HashMap<String, Obj>());
  }
  
  public void pop() {
    mVariables.pop();
  }
  
  private final Stack<Map<String, Obj>> mVariables =
      new Stack<Map<String, Obj>>();
}
