package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ObjectBuiltIns {
  // TODO(bob): Move into reflection module.
  @Signature("(_) class")
  public static class Class_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return arg.getClassObj();
    }
  }
  
  // TODO(bob): Move into reflection module.
  @Signature("(_) is?(class Class)")
  public static class Is implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(
          arg.getField(0).getClassObj().isSubclassOf(arg.getField(1).asClass()));
    }
  }

  // TODO(bob): Move into reflection module.
  @Signature("(_) sameAs?(other)")
  public static class Same implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(
          arg.getField(0) == arg.getField(1));
    }
  }
  
  // TODO(bob): Rename toString.
  @Signature("(_) string")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createString("<" + arg.getClassObj().getName() + ">");
    }
  }
}
