package com.stuffwithstuff.magpie.interpreter;

import java.util.Arrays;
import java.util.HashMap;

import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.interpreter.builtin.BuiltInFunctions;
import com.stuffwithstuff.magpie.interpreter.builtin.BuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.ClassBuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.ClassNew;
import com.stuffwithstuff.magpie.interpreter.builtin.FunctionBuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.IntBuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.ListBuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.ObjectBuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.StringBuiltIns;
import com.stuffwithstuff.magpie.interpreter.builtin.TupleBuiltIns;

public class EnvironmentBuilder {
  public EnvironmentBuilder(Interpreter interpreter, Module module) {
    mInterpreter = interpreter;
    mModule = module;
  }

  public ClassObj createClassClass() {
    Scope scope = mModule.getScope();
    
    // The class of all classes. Its class is itself.
    ClassObj classObj = new ClassObj(null, "Class", null,
        new HashMap<String, Field>(), scope);
    classObj.bindClass(classObj);
    scope.define("Class", classObj);
    
    return classObj;
  }
  
  public void initialize() {
    // Create the default new() method for creating objects.
    Callable newMethod = new ClassNew(mModule.getScope());
    mModule.getScope().define("new", newMethod);
    
    ClassObj indexable = class_("Indexable");
    ClassObj comparable = class_("Comparable");
    
    class_("Bool");
    class_("Int", comparable);
    class_("Function");
    class_("List", indexable);
    class_("Nothing");
    class_("Record");
    class_("String", comparable);
    class_("Tuple", indexable);

    // Define the core error classes.
    ClassObj error = class_("Error");
    
    class_("BadCallError", error);
    class_("AmbiguousMethodError", error);
    class_("IOError", error);
    class_("NoMatchError", error);
    class_("NoMethodError", error);
    class_("NoVariableError", error);
    class_("OutOfBoundsError", error);
    class_("ParentCollisionError", error);
    class_("ParseError", error);
    class_("RedefinitionError", error);
    
    // Register the built-in methods.
    BuiltIns.register(ClassBuiltIns.class, mModule);
    BuiltIns.register(FunctionBuiltIns.class, mModule);
    BuiltIns.register(IntBuiltIns.class, mModule);
    BuiltIns.register(ListBuiltIns.class, mModule);
    BuiltIns.register(ObjectBuiltIns.class, mModule);
    BuiltIns.register(TupleBuiltIns.class, mModule);
    BuiltIns.register(StringBuiltIns.class, mModule);
    BuiltIns.register(BuiltInFunctions.class, mModule);
  }
  
  private ClassObj class_(String name, ClassObj... parents) {
    ClassObj classObj = mInterpreter.createClass(name, Arrays.asList(parents), 
        new HashMap<String, Field>(), mModule.getScope());
    mModule.getScope().define(name, classObj);
    
    return classObj;
  }
  
  private final Interpreter mInterpreter;
  private final Module mModule;
}
