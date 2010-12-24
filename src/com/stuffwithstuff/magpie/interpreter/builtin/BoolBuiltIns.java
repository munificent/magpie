package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class BoolBuiltIns {  
  @Shared
  @Signature("equal?(left Bool, right Bool -> Bool)")
  public static class EqEq implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      boolean left = arg.getTupleField(0).asBool();
      boolean right = arg.getTupleField(1).asBool();
      
      return interpreter.createBool(left == right);
    }
  }
  
  @Getter("not Bool")
  public static class Not implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
  }
  
  @Getter("string String")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
  }
}
