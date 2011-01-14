package com.stuffwithstuff.magpie.interpreter;

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

    RedefinitionHandler handler = new RedefinitionHandler() {
      public void handle(String name) {
        // Cannot redefine a variable in the same scope.
        interpreter.throwError("RedefinitionError");
      }
    };
    
    PatternBinder binder = new PatternBinder(interpreter, context, handler);
    pattern.accept(binder, value);
  }

  public static void bind(final Checker checker, final Position position,
      Pattern pattern, Obj value, EvalContext context) {

    RedefinitionHandler handler = new RedefinitionHandler() {
      public void handle(String name) {
        checker.addError(position,
            "There is already a variable named \"%s\" declared in this scope.",
            name);
      }
    };

    PatternBinder binder = new PatternBinder(checker.getInterpreter(),
        context, handler);
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
    // Do nothing.
    return null;
  }

  @Override
  public Void visit(ValuePattern pattern, Obj value) {
    // Do nothing.
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
      mContext.define(pattern.getName(), value);
    } else {
      // Cannot redefine a variable in the same scope.
      mErrorHandler.handle(pattern.getName());
    }

    return null;
  }

  @Override
  public Void visit(WildcardPattern pattern, Obj value) {
    // Do nothing.
    return null;
  }
  
  private interface RedefinitionHandler {
    void handle(String name);
  }
  
  private PatternBinder(Interpreter interpreter, EvalContext context,
      RedefinitionHandler errorHandler) {
    mInterpreter = interpreter;
    mContext = context;
    mErrorHandler = errorHandler;
  }
  
  private final Interpreter mInterpreter;
  private final EvalContext mContext;
  private final RedefinitionHandler mErrorHandler;
}
