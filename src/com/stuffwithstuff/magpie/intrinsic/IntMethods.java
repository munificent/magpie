package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class IntMethods {
  /*
  @Shared
  @Signature("parse(text String -> Int)")
  public static class Parse implements BuiltInCallable {
    public Obj invoke(Context context, Obj thisObj, Obj arg) {
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
  
  @Def("(left is Int) +(right is Int)")
  @Doc("Adds the two numbers.")
  public static class Add extends ArithmeticOperator {
    protected int perform(int left, int right) { return left + right; }
  }
  
  @Def("(left is Int) -(right is Int)")
  @Doc("Subtracts the two numbers.")
  public static class Subtract extends ArithmeticOperator {
    protected int perform(int left, int right) { return left - right; }
  }
  
  @Def("(left is Int) *(right is Int)")
  @Doc("Multiplies the two numbers.")
  public static class Multiply extends ArithmeticOperator {
    protected int perform(int left, int right) { return left * right; }
  }
  
  @Def("(left is Int) /(right is Int)")
  @Doc("Divides the two numbers.")
  public static class Divide extends ArithmeticOperator {
    protected int perform(int left, int right) { return left / right; }
  }
  
  @Def("(left is Int) %(right is Int)")
  @Doc("Returns left modulo right.")
  public static class Modulo extends ArithmeticOperator {
    protected int perform(int left, int right) { return left % right; }
  }

  private abstract static class ArithmeticOperator implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(perform(left.asInt(), right.asInt()));
    }
    
    protected abstract int perform(int left, int right);
  }
  
  @Def("(left is Int) ==(right is Int)")
  @Doc("Returns true if the two numbers are equal.")
  public static class Equals implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asInt() == right.asInt());
    }
  }
  
  @Def("(left is Int) compareTo(right is Int)")
  @Doc("Returns -1 if left is less than right, 1 if it is greater or 0 if\n" +
       "they are the same.")
  public static class Compare implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(((Integer)left.asInt()).compareTo(right.asInt()));
    }
  }
  
  @Def("(is Int) toString")
  @Doc("Returns a string representation of the number.")
  public static class ToString implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(Integer.toString(left.asInt()));
    }
  }
}
