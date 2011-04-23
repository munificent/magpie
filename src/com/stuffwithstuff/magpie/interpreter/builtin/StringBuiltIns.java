package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class StringBuiltIns {  
  @Signature("(_ String)[index Int]")
  public static class Index implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String string = arg.getField(0).asString();
      int index = arg.getField(1).asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) index = string.length() + index;
      
      if ((index < 0) || (index >= string.length())) {
        interpreter.error(Name.OUT_OF_BOUNDS_ERROR);
      }
      
      return interpreter.createString(string.substring(index, index + 1));
    }
  }

  @Signature("(_ String) count")
  public static class Count implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createInt(arg.asString().length());
    }
  }
  
  @Signature("(_ String) +(right String)")
  public static class Add implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String left = arg.getField(0).asString();
      String right = arg.getField(1).asString();
      
      return interpreter.createString(left + right);
    }
  }
  
  @Signature("(_ String) ==(other String)")
  public static class Equals implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(arg.getField(0).asString().equals(
          arg.getField(1).asString()));
    }
  }
  
  @Signature("(_ String) compareTo(other String)")
  public static class CompareTo implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createInt(arg.getField(0).asString().compareTo(
          arg.getField(1).asString()));
    }
  }
}
