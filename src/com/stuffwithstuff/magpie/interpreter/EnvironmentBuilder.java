package com.stuffwithstuff.magpie.interpreter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.builtin.ClassNew;

public class EnvironmentBuilder {
  public EnvironmentBuilder(Interpreter interpreter, Module module) {
    mInterpreter = interpreter;
    mModule = module;
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
    
    // TODO(bob): These should be in a different module.
    
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
    
    class_("DefineExpr", expr)
      .val("pattern",  "Pattern")
      .val("value",    "Expr")
    .end();
    
    class_("FnExpr", expr)
      .val("pattern",  "Pattern")
      .val("body",     "Expr")
    .end();
    
    class_("ImportExpr", expr)
      .val("module",    "String")
      .val("name")      // String | Nothing
      .val("rename")    // String | Nothing
    .end();
    
    class_("IntExpr", expr)
      .val("value", "Int")
    .end();
    
    class_("ListExpr", expr)
      .val("elements", "List")
    .end();
    
    class_("LoopExpr", expr)
      .val("body",     "Expr")
    .end();
    
    class_("MatchExpr", expr)
      .val("value",    "Expr")
      .val("cases",    "List") // List of MatchCases
    .end();
    
    class_("MethodExpr", expr)
      .val("name",     "String")
      .val("pattern",  "Pattern")
      .val("body",     "Expr")
    .end();
    
    class_("NothingExpr", expr)
    .end();
    
    class_("RecordExpr", expr)
      .val("fields", "List") // List of (Name, Expr)
    .end();
    
    class_("ReturnExpr", expr)
      .val("value", "Expr")
    .end();
    
    class_("ScopeExpr", expr)
      .val("body", "Expr")
      .val("catches", "List") // List of MatchCases
    .end();
    
    class_("SequenceExpr", expr)
      .val("exprs", "List") // List of Exprs
    .end();
    
    class_("StringExpr", expr)
      .val("value", "String")
    .end();
    
    class_("VariableExpr", expr)
      .val("name", "String")
    .end();
    
    
    // Define the pattern classes.
    ClassObj pattern = class_("Pattern")
    .end();
    
    class_("RecordPattern", pattern)
      .val("fields", "List") // List of (Name, Pattern)
    .end();
    
    // TODO(bob): Other patterns, MatchCase
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
    
    public ClassBuilder val(String name, String type) {
      mFields.put(name, new Field(false, null, Pattern.type(Expr.name(type))));
      
      return this;
    }
    
    public ClassBuilder val(String name) {
      mFields.put(name, new Field(false, null, null));
      
      return this;
    }
    
    private final String mName;
    private final ClassObj[] mParents;
    private final Map<String, Field> mFields = new HashMap<String, Field>();
  }
  
  private final Interpreter mInterpreter;
  private final Module mModule;
}
