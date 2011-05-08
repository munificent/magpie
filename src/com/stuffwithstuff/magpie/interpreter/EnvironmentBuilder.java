package com.stuffwithstuff.magpie.interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.intrinsic.ClassNew;

public class EnvironmentBuilder {
  public EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
    mModule = mInterpreter.getBaseModule();
  }

  public ClassObj createClassClass() {
    Scope scope = mModule.getScope();
    
    // The class of all classes. Its class is itself.
    // TODO(bob): Doc.
    ClassObj classObj = new ClassObj(null, Name.CLASS, null,
        new HashMap<String, Field>(), scope, "");
    classObj.bindClass(classObj);
    scope.define(Name.CLASS, classObj);
    
    return classObj;
  }
  
  public void initialize() {
    // Create the default new() method for creating objects.
    Scope scope = mModule.getScope();
    Callable newMethod = new ClassNew(scope);
    scope.define("new", newMethod);
    
    ClassObj indexable = class_("Indexable").end();
    ClassObj comparable = class_("Comparable").end();
    
    class_("Bool").end();
    class_("Int", comparable).end();
    class_("Function").end();
    class_("List", indexable).end();
    class_("Nothing").end();
    class_("Record").end();
    class_("String", comparable).end();
    class_("Tuple", indexable).end();

    // Define the core error classes.
    ClassObj error = class_("Error").end();
    
    class_(Name.AMBIGUOUS_METHOD_ERROR, error).end();
    class_(Name.IO_ERROR, error).end();
    class_(Name.NO_MATCH_ERROR, error).end();
    class_(Name.NO_METHOD_ERROR, error).end();
    class_(Name.NO_VARIABLE_ERROR, error).end();
    class_(Name.OUT_OF_BOUNDS_ERROR, error).end();
    class_(Name.PARENT_COLLISION_ERROR, error).end();
    class_(Name.PARSE_ERROR, error).end();
    class_(Name.REDEFINITION_ERROR, error).end();
  }
  
  private ClassBuilder class_(String name, ClassObj... parents) {
    return new ClassBuilder(name, parents);
  }
  
  private class ClassBuilder {
    public ClassBuilder(String name, ClassObj... parents) {
      mName = name;
      mParents = parents;
    }
    
    public ClassObj end() {
      // TODO(bob): Doc.
      ClassObj classObj = mInterpreter.createClass(mName,
          Arrays.asList(mParents), mFields, mModule.getScope(), "");
      mModule.getScope().define(mName, classObj);
      
      return classObj;
    }
    
    private final String mName;
    private final ClassObj[] mParents;
    private final Map<String, Field> mFields = new HashMap<String, Field>();
  }
  
  private final Interpreter mInterpreter;
  private final Module mModule;
}
