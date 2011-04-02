package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class IntBuiltIns {
  /*
  @Shared
  @Signature("parse(text String -> Int)")
  public static class Parse implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String text = arg.asString();
      
      try {
        int value = Integer.parseInt(text);
        return interpreter.createInt(value);
      } catch (NumberFormatException ex) {
        return interpreter.nothing();
      }
    }
  }
  
  @Shared
  @Signature("equal?(left Int, right Int -> Bool)")
  public static class EqEq implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createBool(left == right);
    }
  }
  */
  
  @Signature("(this Int) +(right Int)")
  public static class Add implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left + right);
    }
  }

  /*
  @Shared
  @Signature("subtract(left Int, right Int -> Int)")
  public static class Subtract implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left - right);
    }
  }
  
  @Shared
  @Signature("multiply(left Int, right Int -> Int)")
  public static class Multiply implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left * right);
    }
  }
  
  @Shared
  @Signature("divide(left Int, right Int -> Int)")
  public static class Divide implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left / right);
    }
  }
  
  @Shared
  @Signature("modulo(left Int, right Int -> Int)")
  public static class Modulo implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(left % right);
    }
  }
  
  @Shared
  @Signature("lessThan?(left Int, right Int -> Bool)")
  public static class LessThan implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createBool(left < right);
    }
  }
  */

  @Signature("(this Int) string")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createString(Integer.toString(arg.asInt()));
    }
  }
}
