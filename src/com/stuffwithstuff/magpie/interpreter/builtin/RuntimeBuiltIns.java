package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.interpreter.CheckError;
import com.stuffwithstuff.magpie.interpreter.Checker;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class RuntimeBuiltIns {
  @Shared
  // TODO(bob): Should be declared to return array of strings.
  @Signature("checkClass(classObj Class)")
  public static Obj checkClass(Interpreter interpreter, Obj thisObj, Obj arg) {
    Checker checker = new Checker(interpreter);
    
    checker.checkClass((ClassObj)arg);
    
    return translateErrors(interpreter, checker.getErrors());
  }

  @Shared
  // TODO(bob): Should be declared to return array of strings.
  @Signature("checkFunction(function)")
  public static Obj checkFunction(Interpreter interpreter, Obj thisObj, Obj arg) {
    Checker checker = new Checker(interpreter);
    
    FnObj function = (FnObj)arg;
    checker.checkFunction(function.getFunction(), function.getClosure(),
        interpreter.getNothingType());
    
    return translateErrors(interpreter, checker.getErrors());
  }

  private static Obj translateErrors(Interpreter interpreter, List<CheckError> errors) {
    List<Obj> errorObjs = new ArrayList<Obj>();
    for (CheckError error : errors) {
      // TODO(bob): Should eventually return more than just the error message.
      errorObjs.add(interpreter.createString(error.getMessage()));
    }

    return interpreter.createArray(errorObjs);
  }
}
