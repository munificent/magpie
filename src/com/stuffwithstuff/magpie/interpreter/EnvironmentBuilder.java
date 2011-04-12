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
  public EnvironmentBuilder(Interpreter interpreter, Scope scope) {
    mInterpreter = interpreter;
    mScope = scope;
  }

  public ClassObj createClassClass() {
    // The class of all classes. Its class is itself.
    ClassObj classObj = new ClassObj(null, "Class", null,
        new HashMap<String, Field>(), mScope);
    classObj.bindClass(classObj);
    mScope.define("Class", classObj);
    return classObj;
  }
  
  public void initialize() {    
    // Create the default new() method for creating objects.
    Multimethod.define(mScope, "new").addMethod(new ClassNew(mScope));

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
    class_("OutOfBoundsError", error);
    class_("ParentCollisionError", error);
    class_("ParseError", error);
    class_("RedefinitionError", error);
    class_("UnknownVariableError", error);
    
    // Register the built-in methods.
    BuiltIns.register(ClassBuiltIns.class, mScope);
    BuiltIns.register(FunctionBuiltIns.class, mScope);
    BuiltIns.register(IntBuiltIns.class, mScope);
    BuiltIns.register(ListBuiltIns.class, mScope);
    BuiltIns.register(ObjectBuiltIns.class, mScope);
    BuiltIns.register(TupleBuiltIns.class, mScope);
    BuiltIns.register(StringBuiltIns.class, mScope);
    BuiltIns.register(BuiltInFunctions.class, mScope);
  }
  
  private ClassObj class_(String name, ClassObj... parents) {
    ClassObj classObj = mInterpreter.createClass(name, Arrays.asList(parents), 
        new HashMap<String, Field>(), mScope);
    mScope.define(name, classObj);
    return classObj;
  }
  
  private final Interpreter mInterpreter;
  private final Scope mScope;
}
