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
  */
  
  @Signature("(is Int) +(right is Int)")
  public static class Add extends ArithmeticOperator {
    protected int perform(int left, int right) { return left + right; }
  }
  
  @Signature("(is Int) -(right is Int)")
  public static class Subtract extends ArithmeticOperator {
    protected int perform(int left, int right) { return left - right; }
  }
  
  @Signature("(is Int) *(right is Int)")
  public static class Multiply extends ArithmeticOperator {
    protected int perform(int left, int right) { return left * right; }
  }
  
  @Signature("(is Int) /(right is Int)")
  public static class Divide extends ArithmeticOperator {
    protected int perform(int left, int right) { return left / right; }
  }
  
  @Signature("(is Int) %(right is Int)")
  public static class Modulo extends ArithmeticOperator {
    protected int perform(int left, int right) { return left % right; }
  }

  private abstract static class ArithmeticOperator implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      int left = arg.getField(0).asInt();
      int right = arg.getField(1).asInt();
      
      return interpreter.createInt(perform(left, right));
    }
    
    protected abstract int perform(int left, int right);
  }
  
  @Signature("(is Int) ==(right is Int)")
  public static class Equals implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      int left = arg.getField(0).asInt();
      int right = arg.getField(1).asInt();
      
      return interpreter.createBool(left == right);
    }
  }
  
  @Signature("(is Int) compareTo(other is Int)")
  public static class Compare implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      int left = arg.getField(0).asInt();
      int right = arg.getField(1).asInt();
      
      return interpreter.createInt(((Integer)left).compareTo(right));
    }
  }
  
  @Signature("(is Int) string")
  public static class String_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createString(Integer.toString(arg.asInt()));
    }
  }
}
