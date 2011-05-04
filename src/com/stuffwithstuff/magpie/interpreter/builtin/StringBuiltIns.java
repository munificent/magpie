package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class StringBuiltIns {  
  @Signature("(is String)[index is Int]")
  public static class Index implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String string = arg.getField(0).asString();
      int index = arg.getField(1).asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) index = string.length() + index;
      
      if ((index < 0) || (index >= string.length())) {
        interpreter.error(Name.OUT_OF_BOUNDS_ERROR, "Index " + index +
            " is out of bounds [0, " + string.length() + "].");
      }
      
      return interpreter.createString(string.substring(index, index + 1));
    }
  }

  @Signature("(is String) count")
  public static class Count implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createInt(arg.asString().length());
    }
  }
  
  @Signature("(is String) +(right is String)")
  public static class Add implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String left = arg.getField(0).asString();
      String right = arg.getField(1).asString();
      
      return interpreter.createString(left + right);
    }
  }
  
  @Signature("(is String) ==(other is String)")
  public static class Equals implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(arg.getField(0).asString().equals(
          arg.getField(1).asString()));
    }
  }
  
  @Signature("(is String) compareTo(other is String)")
  public static class CompareTo implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createInt(arg.getField(0).asString().compareTo(
          arg.getField(1).asString()));
    }
  }
}
