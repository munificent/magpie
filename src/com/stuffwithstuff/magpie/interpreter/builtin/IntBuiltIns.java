package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class IntBuiltIns {
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
  
  @Signature("+(other Int -> Int)")
  public static class Add implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
  }
  
  @Signature("-(other Int -> Int)")
  public static class Subtract implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
  }
  
  @Signature("*(other Int -> Int)")
  public static class Multiply implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
  }
  
  @Signature("/(other Int -> Int)")
  public static class Divide implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
  }
  
  @Signature("==(other Int -> Bool)")
  public static class EqEq implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
  }
  
  @Signature("!=(other Int -> Bool)")
  public static class NotEqual implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
  }
  
  @Signature("<(other Int -> Bool)")
  public static class Less implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
  }
  
  @Signature(">(other Int -> Bool)")
  public static class Greater implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
  }
  
  @Signature("<=(other Int -> Bool)")
  public static class LessOrEqual implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
  }
  
  @Signature(">=(other Int -> Bool)")
  public static class GreaterOrEqual implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
  }
  
  @Getter("string(-> String)")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Integer.toString(thisObj.asInt()));
    }
  }
}
