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
  public static class Add extends ArithmeticOperator {
    protected int perform(int left, int right) { return left + right; }
  }
  
  @Signature("(this Int) -(right Int)")
  public static class Subtract extends ArithmeticOperator {
    protected int perform(int left, int right) { return left - right; }
  }
  
  @Signature("(this Int) *(right Int)")
  public static class Multiply extends ArithmeticOperator {
    protected int perform(int left, int right) { return left * right; }
  }
  
  @Signature("(this Int) /(right Int)")
  public static class Divide extends ArithmeticOperator {
    protected int perform(int left, int right) { return left / right; }
  }
  
  @Signature("(this Int) %(right Int)")
  public static class Modulo extends ArithmeticOperator {
    protected int perform(int left, int right) { return left % right; }
  }

  private abstract static class ArithmeticOperator implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createInt(perform(left, right));
    }
    
    protected abstract int perform(int left, int right);
  }
  
  @Signature("(this Int) ==(right Int)")
  public static class Equals extends ComparisonOperator {
    protected boolean perform(int left, int right) { return left == right; }
  }
  
  @Signature("(this Int) <(right Int)")
  public static class LessThan extends ComparisonOperator {
    protected boolean perform(int left, int right) { return left < right; }
  }
  
  @Signature("(this Int) >(right Int)")
  public static class RightThan extends ComparisonOperator {
    protected boolean perform(int left, int right) { return left > right; }
  }
  
  @Signature("(this Int) <=(right Int)")
  public static class LessThanOrEqual extends ComparisonOperator {
    protected boolean perform(int left, int right) { return left <= right; }
  }
  
  @Signature("(this Int) >=(right Int)")
  public static class GreaterThanOrEqual extends ComparisonOperator {
    protected boolean perform(int left, int right) { return left >= right; }
  }
  
  private abstract static class ComparisonOperator implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      int left = arg.getTupleField(0).asInt();
      int right = arg.getTupleField(1).asInt();
      
      return interpreter.createBool(perform(left, right));
    }
    
    protected abstract boolean perform(int left, int right);
  }
  
  /*
  
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
