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
    
    newClass("BadCallError")
        .inherit("Error");
    
    newClass("NoMethodError")
        .inherit("Error");
    
    // Define the core AST classes. We set these up here instead of in base
    // because they will need to be available immediately by the parser so that
    // quotations can be converted to Magpie.
    newClass("Position")
        .field("file",       name("String"))
        .field("startLine",  name("Int"))
        .field("startCol",   name("Int"))
        .field("endLine",    name("Int"))
        .field("endCol",     name("Int"));

    newClass("Expression");
    
    newClass("AssignExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("receiver", or(name("Expression"), name("Nothing")))
        .field("name",     name("String"))
        .field("value",    name("Expression"));
    
    newClass("BlockExpression")
        .inherit("Expression")
        .field("expressions", list(name("Expression")))
        .field("catchExpression", or(name("Expression"), name("Nothing")));
    
    newClass("BoolExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("value", name("Bool"));

    newClass("BreakExpression")
        .inherit("Expression")
        .field("position", name("Position"));

    newClass("CallExpression")
        .inherit("Expression")
        .field("target",   name("Expression"))
        .field("argument", name("Expression"));

    newClass("FunctionExpression")
        .inherit("Expression")
        .field("position",     name("Position"))
        .field("pattern",      name("Pattern"))
        .field("body",         name("Expression"));

    newClass("IntExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("value",    name("Int"));
    
    newClass("LoopExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("body",     name("Expression"));

    newClass("MatchCase")
        .inherit("Expression")
        .field("pattern", name("Pattern"))
        .field("body",    name("Expression"));

    newClass("MatchExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("value",    name("Expression"))
        .field("cases",    list(name("MatchCase")));

    newClass("MessageExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("receiver", or(name("Expression"), name("Nothing")))
        .field("name",     Expr.name("String"));
    
    newClass("NothingExpression")
        .inherit("Expression")
        .field("position", name("Position"));

    newClass("QuotationExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("body",     name("Expression"));
    
    newClass("RecordExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("fields",   list(name("String"), name("Expression")));

    newClass("ReturnExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("value",    name("Expression"));
    
    newClass("ScopeExpression")
        .inherit("Expression")
        .field("body", name("Expression"));
    
    newClass("StringExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("value",    name("String"));

    newClass("ThisExpression")
        .inherit("Expression")
        .field("position", name("Position"));

    newClass("TupleExpression")
        .inherit("Expression")
        .field("fields", list(name("Expression")));
    
    newClass("UnquoteExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("body",     name("Expression"));
    
    newClass("UsingExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("name",     name("String"));

    newClass("VariableExpression")
        .inherit("Expression")
        .field("position", name("Position"))
        .field("pattern",  name("Pattern"))
        .field("value",    name("Expression"));
  }
  
  private EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  private ClassBuilder newClass(String name) {
    return new ClassBuilder(mInterpreter.createGlobalClass(name));
  }
  
  private Expr list(Expr... exprs) {
    // TODO(bob): Generic types aren't supported right now since the type system
    // got yanked. Instead, we'll just return null which allows the field to
    // be any type. (Specifically, the "new" method that takes a record will
    // allow any type in the record field for this field.
    return null;
    /*
    Expr arg;
    if (exprs.length > 1) {
      arg = Expr.tuple(exprs);
    } else {
      arg = exprs[0];
    }
    
    return Expr.call(Expr.name("List"), arg);
    */
  }
  
  private Expr name(String name) {
    return Expr.name(name);
  }
  
  private Expr or(Expr... exprs) {
    // TODO(bob): Or types aren't supported right now since the type system
    // got yanked. Instead, we'll just return null which allows the field to
    // be any type. (Specifically, the "new" method that takes a record will
    // allow any type in the record field for this field.
    return null;
    
    /*
    Expr result = exprs[0];
    for (int i = 1; i < exprs.length; i++) {
      result = Expr.message(exprs[i].getPosition(), null, "|", Expr.tuple(result, exprs[i]));
    }
    return result;
    */
  }

  private class ClassBuilder {
    public ClassBuilder(ClassObj classObj) {
      mClassObj = classObj;
    }
    
    public ClassBuilder inherit(String name) {
      Obj parent = mInterpreter.getGlobal(name);
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
