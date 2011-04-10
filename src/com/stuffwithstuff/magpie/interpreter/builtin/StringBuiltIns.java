package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class StringBuiltIns {  
  @Signature("(_ String)[index Int]")
  public static class Index implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String string = arg.getTupleField(0).asString();
      int index = arg.getTupleField(1).asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) index = string.length() + index;
      
      if ((index < 0) || (index >= string.length())) {
        interpreter.error("OutOfBoundsError");
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
      String left = arg.getTupleField(0).asString();
      String right = arg.getTupleField(1).asString();
      
      return interpreter.createString(left + right);
    }
  }

  @Signature("(_ String) ==(other String)")
  public static class Equals implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(arg.getTupleField(0).asString().equals(
          arg.getTupleField(1).asString()));
    }
  }
  
  @Signature("(_ String) compareTo(other String)")
  public static class CompareTo implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createInt(arg.getTupleField(0).asString().compareTo(
          arg.getTupleField(1).asString()));
    }
  }
  
  /*
  @Signature("contains?(other String -> Bool)")
  public static class Contains implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String left = thisObj.asString();
      String right = arg.asString();
      
      return interpreter.createBool(left.contains(right));
    }
  }
  
  @Signature("split(delimiter String -> List(String))")
  public static class Split implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String string = thisObj.asString();
      String delimiter = arg.asString();
      
      // Note: We're not using String#split because we don't want to split on
      // a regex, just a literal delimiter.
      List<Obj> substrings = new ArrayList<Obj>();
      int start = 0;
      while (start != -1) {
        int end = string.indexOf(delimiter, start);
        String substring;
        if (end != -1) {
          substring = string.substring(start, end);
          start = end + delimiter.length();
        } else {
          substring = string.substring(start);
          start = -1;
        }
        substrings.add(interpreter.createString(substring));
      }
      
      return interpreter.createArray(substrings);
    }
  }

  @Signature("substring(arg -> String)")
  public static class Substring implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // TODO(bob): Hackish way to see if we have one or two arguments to this.
      if (arg.getTupleField(0) != null) {
        int startIndex = arg.getTupleField(0).asInt();
        int endIndex = arg.getTupleField(1).asInt();
        String substring = thisObj.asString().substring(startIndex, endIndex);
        return interpreter.createString(substring);
      } else {
        int startIndex = arg.asInt();
        String substring = thisObj.asString().substring(startIndex);
        return interpreter.createString(substring);
      }
    }
  }
  */
}
