package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.pattern.*;

/**
 * Given a pattern, a value, and a context, destructures the value and binds
 * new variables in the context.
 */
public class PatternBinder extends PatternBinderBase {
  public static void bind(Interpreter interpreter, Pattern pattern,
      Obj value, EvalContext context) {
    PatternBinder binder = new PatternBinder(interpreter, context);
    pattern.accept(binder, value);
  }

  @Override
  public Void visit(VariablePattern pattern, Obj value) {
    // Bind the variable.
    if (!bindNewVariable(pattern.getName(), value)) {
      // Cannot redefine a variable in the same scope.
      getInterpreter().error("RedefinitionError",
          String.format("There is already a variable named \"%s\" in this scope.", pattern.getName()));
    }

    return null;
  }

  private PatternBinder(Interpreter interpreter, EvalContext context) {
    super(interpreter, context);
  }
}
