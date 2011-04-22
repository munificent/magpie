package com.stuffwithstuff.magpie.interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
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
    
    class_("BadCallError", error).end();
    class_("AmbiguousMethodError", error).end();
    class_("IOError", error).end();
    class_("NoMatchError", error).end();
    class_("NoMethodError", error).end();
    class_("NoVariableError", error).end();
    class_("OutOfBoundsError", error).end();
    class_("ParentCollisionError", error).end();
    class_("ParseError", error).end();
    class_("RedefinitionError", error).end();
    
    // Define the AST classes.
    class_("SourceLocation")
      .val("path",      "String")
      .val("startLine", "Int")
      .val("startCol",  "Int")
      .val("endLine",   "Int")
      .val("startCol",  "Int")
    .end();
    
    ClassObj expr = class_("Expr")
      .val("location",   "SourceLocation")
    .end();
    
    class_("AssignExpr", expr)
      .val("name", "String")
      .val("value", "Expr")
    .end();
    
    class_("BoolExpr", expr)
      .val("value", "Bool")
    .end();
    
    class_("BreakExpr", expr)
    .end();
    
    class_("CallExpr", expr)
      .val("receiver", "Expr")
      .val("name",     "String")
      .val("arg",      "Expr")
    .end();
    
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
  
  private ClassBuilder class_(String name, ClassObj... parents) {
    return new ClassBuilder(name, parents);
  }
  
  private class ClassBuilder {
    public ClassBuilder(String name, ClassObj... parents) {
      mName = name;
      mParents = parents;
    }
    
    public ClassObj end() {
      ClassObj classObj = mInterpreter.createClass(mName,
          Arrays.asList(mParents), mFields, mModule.getScope());
      mModule.getScope().define(mName, classObj);
      
      return classObj;
    }
    
    public ClassBuilder val(String name, String type) {
      mFields.put(name, new Field(false, null, Expr.variable(type)));
      
      return this;
    }
    
    private final String mName;
    private final ClassObj[] mParents;
    private final Map<String, Field> mFields = new HashMap<String, Field>();
  }
  
  private final Interpreter mInterpreter;
  private final Module mModule;
}
