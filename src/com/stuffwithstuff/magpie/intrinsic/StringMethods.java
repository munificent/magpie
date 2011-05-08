package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class StringMethods {  
  @Def("(is String)[index is Int]")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      String string = arg.getField(0).asString();
      int index = arg.getField(1).asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) index = string.length() + index;
      
      if ((index < 0) || (index >= string.length())) {
        context.error(Name.OUT_OF_BOUNDS_ERROR, "Index " + index +
            " is out of bounds [0, " + string.length() + "].");
      }
      
      return context.toObj(string.substring(index, index + 1));
    }
  }

  @Def("(is String) count")
  public static class Count implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj(arg.asString().length());
    }
  }
  
  @Def("(is String) +(right is String)")
  public static class Add implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      String left = arg.getField(0).asString();
      String right = arg.getField(1).asString();
      
      return context.toObj(left + right);
    }
  }
  
  @Def("(is String) ==(other is String)")
  public static class Equals implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj(arg.getField(0).asString().equals(
          arg.getField(1).asString()));
    }
  }
  
  @Def("(is String) compareTo(other is String)")
  public static class CompareTo implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      return context.toObj(arg.getField(0).asString().compareTo(
          arg.getField(1).asString()));
    }
  }

  // TODO(bob): Make an indexer that takes a range, so you can do:
  // "some string"[2 to(4)]
  @Def("(is String) substring(start is Int, stop is Int)")
  public static class Substring implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      String string = arg.getField(0).asString();
      int startIndex = arg.getField(1).getField(0).asInt();
      int endIndex = arg.getField(1).getField(1).asInt();
      return context.toObj(string.substring(startIndex, endIndex));
    }
  }
}
