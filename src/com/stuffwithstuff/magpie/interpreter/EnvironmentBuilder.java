package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.Expr;

public class EnvironmentBuilder {
  public static void initialize(Interpreter interpreter) {
    EnvironmentBuilder builder = new EnvironmentBuilder(interpreter);
    builder.initialize();
  }
  
  public void initialize() {
    // Define the core AST classes. We set these up here instead of in base
    // because they will need to be available immediately by the parser so that
    // quotations can be converted to Magpie.
    newClass("AssignExpression")
        .field("receiver", or(name("Expression"), name("Nothing")))
        .field("name",     name("String"))
        .field("value",    name("Expression"));
    
    newClass("BlockExpression")
        .field("expressions", list(name("Expression")))
        .field("catchExpression", or(name("Expression"), name("Nothing")));
    
    newClass("BoolExpression")
        .field("value", name("Bool"));

    newClass("BreakExpression");

    newClass("CallExpression")
        .field("target",   name("Expression"))
        .field("typeArgs", list(name("Expression")))
        .field("argument", name("Expression"));

    newClass("FunctionTypeExpression")
        .field("typeParams", list(name("String"), name("Expression")))
        .field("pattern",    name("Pattern"))
        .field("returnType", name("Expression"));
    
    newClass("FunctionExpression")
        .field("functionType", name("FunctionTypeExpression"))
        .field("body",         name("Expression"));

    newClass("IfExpression")
        .field("name",      or(name("String"), name("Nothing")))
        .field("condition", name("Expression"))
        .field("thenArm",   name("Expression"))
        .field("elseArm",   name("Expression"));

    newClass("IntExpression")
        .field("value", name("Int"));
    
    newClass("LoopExpression")
        .field("body", name("Expression"));

    newClass("MatchCase")
        .field("pattern", name("Pattern"))
        .field("body",    name("Expression"));

    newClass("MatchExpression")
        .field("value", name("Expression"))
        .field("cases", list(name("MatchCase")));

    newClass("MessageExpression")
        .field("receiver", or(name("Expression"), name("Nothing")))
        .field("name",     Expr.name("String"));
    
    newClass("NothingExpression");

    newClass("QuotationExpression")
        .field("body", name("Expression"));
    
    newClass("RecordExpression")
        .field("fields", list(name("String"), name("Expression")));

    newClass("ReturnExpression")
        .field("value", name("Expression"));
    
    newClass("ScopeExpression")
        .field("body", name("Expression"));
    
    newClass("StringExpression")
        .field("value", name("String"));

    newClass("ThisExpression");

    newClass("TupleExpression")
        .field("fields", list(name("Expression")));
    
    newClass("TypeofExpression")
        .field("body", name("Expression"));

    newClass("UnsafeCastExpression")
        .field("type",  name("Expression"))
        .field("value", name("Expression"));

    newClass("VariableExpression")
        .field("pattern", name("Pattern"))
        .field("value",   name("Expression"));
  }
  
  private EnvironmentBuilder(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  private ClassBuilder newClass(String name) {
    ClassObj classObj = mInterpreter.createClass(name);
    mInterpreter.getGlobals().define(name, classObj);
    
    return new ClassBuilder(classObj);
  }
  
  private Expr list(Expr... exprs) {
    Expr arg;
    if (exprs.length > 1) {
      arg = Expr.tuple(exprs);
    } else {
      arg = exprs[0];
    }
    
    return Expr.call(Expr.name("List"), arg);
  }
  
  private Expr name(String name) {
    return Expr.name(name);
  }
  
  private Expr or(Expr... exprs) {
    Expr result = exprs[0];
    for (int i = 1; i < exprs.length; i++) {
      result = Expr.message(null, "|", Expr.tuple(result, exprs[i]));
    }
    return result;
  }

  private static class ClassBuilder {
    public ClassBuilder(ClassObj classObj) {
      mClassObj = classObj;
    }
    
    public ClassBuilder field(String name, Expr type) {
      mClassObj.declareField(name, false, type);
      return this;
    }
    
    private ClassObj mClassObj;
  }
  
  private final Interpreter mInterpreter;
}
