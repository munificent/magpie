package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.Expr;

public class EnvironmentBuilder {
  public static void initialize(Interpreter interpreter) {
    EnvironmentBuilder builder = new EnvironmentBuilder(interpreter);
    builder.initialize();
  }
  
  public void initialize() {
    // Define the core error classes.
    newClass("Error");
    
    newClass("BadCallError").inherit("Error");
    
    newClass("NoMethodError").inherit("Error");
  }
  
  private EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  private ClassBuilder newClass(String name) {
    return new ClassBuilder(mInterpreter.createGlobalClass(name));
  }

  private class ClassBuilder {
    public ClassBuilder(ClassObj classObj) {
      mClassObj = classObj;
    }
    
    public ClassBuilder inherit(String name) {
      ClassObj parent = (ClassObj)mInterpreter.getGlobal(name);
      mClassObj.getParents().add(parent);
      return this;
    }
    
    public ClassBuilder field(String name, Expr type) {
      mClassObj.declareField(name, type);
      return this;
    }
    
    private ClassObj mClassObj;
  }
  
  private final Interpreter mInterpreter;
}
