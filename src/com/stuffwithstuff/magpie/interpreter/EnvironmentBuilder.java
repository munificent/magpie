package com.stuffwithstuff.magpie.interpreter;

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
    newClass("UnknownVariableError", "Error");
  }
  
  private EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  private ClassBuilder newClass(String name) {
    return new ClassBuilder(mInterpreter.createGlobalClass(name));
  }
  
  private ClassBuilder newClass(String name, String parentName) {
    ClassObj[] parents = new ClassObj[1];
    parents[0] = (ClassObj)mInterpreter.getGlobal(parentName);
    
    return new ClassBuilder(mInterpreter.createGlobalClass(name, parents));
  }

  private class ClassBuilder {
    public ClassBuilder(ClassObj classObj) {
      mClassObj = classObj;
    }
    
    @SuppressWarnings("unused")
    private ClassObj mClassObj;
  }
  
  private final Interpreter mInterpreter;
}
