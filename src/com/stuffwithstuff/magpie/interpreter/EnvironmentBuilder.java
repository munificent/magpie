package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentBuilder {
  public static void initialize(Interpreter interpreter) {
    EnvironmentBuilder builder = new EnvironmentBuilder(interpreter);
    builder.initialize();
  }
  
  public void initialize() {
    // Define the core error classes.
    newClass("Error");
    
    newClass("BadCallError", "Error");
    newClass("AmbiguousMethodError", "Error");
    newClass("IOError", "Error");
    newClass("NoMatchError", "Error");
    newClass("NoMethodError", "Error");
    newClass("OutOfBoundsError", "Error");
    newClass("ParentCollisionError", "Error");
    newClass("ParseError", "Error");
    newClass("RedefinitionError", "Error");
  }
  
  private EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  private ClassBuilder newClass(String name) {
    return new ClassBuilder(mInterpreter.createGlobalClass(name));
  }
  
  private ClassBuilder newClass(String name, String parentName) {
    List<ClassObj> parents = new ArrayList<ClassObj>();
    parents.add((ClassObj)mInterpreter.getGlobal(parentName));
    
    return new ClassBuilder(mInterpreter.createGlobalClass(name, parents));
  }

  private class ClassBuilder {
    public ClassBuilder(ClassObj classObj) {
      mClassObj = classObj;
    }
    
    private ClassObj mClassObj;
  }
  
  private final Interpreter mInterpreter;
}
