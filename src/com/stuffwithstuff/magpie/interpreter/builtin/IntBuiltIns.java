package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class IntBuiltIns {
  @Shared
  @Signature("parse(text String -> Int)")
  public static Obj parse(Interpreter interpreter, Obj thisObj, Obj arg) {
    String text = arg.asString();
    
    try {
      int value = Integer.parseInt(text);
      return interpreter.createInt(value);
    } catch (NumberFormatException ex) {
      return interpreter.nothing();
    }
  }

  @Signature("+(other Int -> Int)")
  public static Obj _plus_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createInt(left + right);
  }

  @Signature("-(other Int -> Int)")
  public static Obj _sub_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createInt(left - right);
  }

  @Signature("*(other Int -> Int)")
  public static Obj _mult_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createInt(left * right);
  }

  @Signature("/(other Int -> Int)")
  public static Obj _div_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createInt(left / right);
  }

  @Signature("==(other Int -> Bool)")
  public static Obj _eqeq_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createBool(left == right);
  }

  @Signature("!=(other Int -> Bool)")
  public static Obj _nteq_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createBool(left != right);
  }

  @Signature("<(other Int -> Bool)")
  public static Obj _lt_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createBool(left < right);
  }

  @Signature(">(other Int -> Bool)")
  public static Obj _gt_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createBool(left > right);
  }

  @Signature("<=(other Int -> Bool)")
  public static Obj _lteq_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createBool(left <= right);
  }

  @Signature(">=(other Int -> Bool)")
  public static Obj _gteq_(Interpreter interpreter, Obj thisObj, Obj arg) {
    int left = thisObj.asInt();
    int right = arg.asInt();
    
    return interpreter.createBool(left >= right);
  }

  @Getter("toString(-> String)")
  public static Obj toString(Interpreter interpreter, Obj thisObj, Obj arg) {
    return interpreter.createString(Integer.toString(thisObj.asInt()));
  }
}
