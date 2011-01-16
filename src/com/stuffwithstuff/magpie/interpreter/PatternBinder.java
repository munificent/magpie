package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.*;
import com.stuffwithstuff.magpie.parser.Position;

/**
 * Given a pattern, a value, and a context, destructures the value and binds
 * new variables in a new child context.
 * 
 * @author bob
 */
public class PatternBinder implements PatternVisitor<Void, Obj> {
  public static void bind(final Interpreter interpreter, Pattern pattern,
      Obj value, EvalContext context) {
    PatternBinder binder = new PatternBinder(null, interpreter,
        Position.none(), context);
    pattern.accept(binder, value);
  }

  public static void bind(final Checker checker, final Position position,
      Pattern pattern, Obj value, EvalContext context) {
    PatternBinder binder = new PatternBinder(checker, checker.getInterpreter(),
        position, context);
    pattern.accept(binder, value);
  }

  @Override
  public Void visit(TuplePattern pattern, Obj value) {
    // Destructure each field.
    for (int i = 0; i < pattern.getFields().size(); i++) {
      Pattern fieldPattern = pattern.getFields().get(i);
      Obj field = mInterpreter.getMember(Position.none(), value, "_" + i);
      fieldPattern.accept(this, field);
    }
    
    return null;
  }

  @Override
  public Void visit(TypePattern pattern, Obj value) {
    // If we're type-checking, make sure the type of the pattern's value is
    // compatible with the type being matched.
    if (mChecker != null) {
      Obj matchedType = mInterpreter.evaluate(pattern.getType());
      mChecker.checkTypes(value, matchedType, Position.none(),
          "Cannot match a value of type %s against type %s.");
    }

    return null;
  }

  @Override
  public Void visit(ValuePattern pattern, Obj value) {
    // If we're type-checking, make sure the type of the pattern's value is
    // compatible with the type being matched.
    if (mChecker != null) {
      Obj matchedType = mChecker.evaluateExpressionType(pattern.getValue());
      mChecker.checkTypes(value, matchedType, Position.none(),
          "Cannot match a value of type %s against a pattern of type %s.");
    }

    return null;
  }

  @Override
  public Void visit(VariablePattern pattern, Obj value) {
    // If we have a pattern for the variable, recurse into it.
    if (pattern.getPattern() != null) {
      pattern.getPattern().accept(this, value);
    }
    
    // Bind the variable.
    if (mContext.lookUpHere(pattern.getName()) == null) {
      // If the variable has a type, and we're type-checking, use that type
      // instead.
      if ((mChecker != null) && (pattern.getPattern() instanceof TypePattern)) {
        Expr type = ((TypePattern)pattern.getPattern()).getType();
        value = mInterpreter.evaluate(type);
      }
      
      mContext.define(pattern.getName(), value);
    } else {
      // Cannot redefine a variable in the same scope.
      if (mChecker != null) {
        mChecker.addError(mPosition,
            "There is already a variable named \"%s\" declared in this scope.",
            pattern.getName());
      } else {
        mInterpreter.throwError("RedefinitionError");
      }
    }

    return null;
  }

  @Override
  public Void visit(WildcardPattern pattern, Obj value) {
    // Do nothing.
    return null;
  }
  
  private PatternBinder(Checker checker, Interpreter interpreter,
      Position position, EvalContext context) {
    mChecker = checker;
    mInterpreter = interpreter;
    mPosition = position;
    mContext = context;
  }
  
  private final Checker mChecker;
  private final Interpreter mInterpreter;
  private final Position mPosition;
  private final EvalContext mContext;
}
