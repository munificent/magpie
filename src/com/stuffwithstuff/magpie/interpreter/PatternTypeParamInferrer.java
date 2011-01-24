package com.stuffwithstuff.magpie.interpreter;

import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.ast.pattern.*;

/**
 * Given a pattern, an object that matches that pattern, and a set of type
 * parameter names, figures out the type arguments for those parameters.
 */
public class PatternTypeParamInferrer extends PatternBinderBase {
  public static void infer(Interpreter interpreter, 
      FunctionType type, Obj argType, Map<String, Obj> typeArgs) {
    
    PatternTypeParamInferrer inferrer = new PatternTypeParamInferrer(
        interpreter, typeArgs);
    
    type.getPattern().accept(inferrer, argType);
    
    // TODO(bob): Make sure we have a type arg for all type params.
    
    // TODO(bob): Check type args against constraints.
  }
  
  @Override
  public Void visit(VariablePattern pattern, Obj valueType) {
    // See if we found a type param.
    if (pattern.getType() != null) {
      // TODO(bob): Recurse into more complex type expressions.
      Expr type = pattern.getType();
      ExprTypeParamInferrer.infer(getInterpreter(), mTypeArgs, type, valueType);
    }

    return null;
  }
  
  private PatternTypeParamInferrer(Interpreter interpreter,
      Map<String, Obj> typeArgs) {
    super(interpreter, null);
    mTypeArgs = typeArgs;
  }
  
  private final Map<String, Obj> mTypeArgs;
}
