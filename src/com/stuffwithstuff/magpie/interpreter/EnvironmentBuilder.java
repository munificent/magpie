package com.stuffwithstuff.magpie.interpreter;

public class EnvironmentBuilder {
  public static void initialize(Interpreter interpreter) {
    EnvironmentBuilder builder = new EnvironmentBuilder(interpreter);
    builder.initialize();
  }
  
  public void initialize() {
    // Define the core error classes.
    ClassObj error = class_("Error");
    
    class_("BadCallError", error);
    class_("AmbiguousMethodError", error);
    class_("IOError", error);
    class_("NoMatchError", error);
    class_("NoMethodError", error);
    class_("OutOfBoundsError", error);
    class_("ParentCollisionError", error);
    class_("ParseError", error);
    class_("RedefinitionError", error);
    class_("UnknownVariableError", error);
  }
  
  private EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  private ClassObj class_(String name, ClassObj... parents) {
    return mInterpreter.createGlobalClass(name, parents);
  }
  
  private final Interpreter mInterpreter;
}
