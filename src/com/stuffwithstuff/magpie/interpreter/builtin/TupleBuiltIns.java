package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class TupleBuiltIns {
  @Signature("(_ Tuple)[index Int]")
  public static class Index implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg.getTupleField(0),
          arg.getTupleField(1).asInt());
    }
  }

  @Signature("(_ Tuple) count")
  public static class Count implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createInt(arg.asList().size());
    }
  }

  @Signature("(_ Tuple) field0")
  public static class Field0 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 0);
    }
  }
  
  @Signature("(_ Tuple) field1")
  public static class Field1 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 1);
    }
  }
  
  @Signature("(_ Tuple) field2")
  public static class Field2 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 2);
    }
  }
  
  @Signature("(_ Tuple) field3")
  public static class Field3 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 3);
    }
  }
  
  @Signature("(_ Tuple) field4")
  public static class Field4 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 4);
    }
  }
  
  @Signature("(_ Tuple) field5")
  public static class Field5 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 5);
    }
  }
  
  @Signature("(_ Tuple) field6")
  public static class Field6 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 6);
    }
  }
  
  @Signature("(_ Tuple) field7")
  public static class Field7 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 7);
    }
  }
  
  @Signature("(_ Tuple) field8")
  public static class Field8 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 8);
    }
  }
  
  @Signature("(_ Tuple) field9")
  public static class Field9 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 9);
    }
  }
  @Signature("(_ Tuple) field10")
  public static class Field10 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 10);
    }
  }
  
  @Signature("(_ Tuple) field11")
  public static class Field11 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 11);
    }
  }
  
  @Signature("(_ Tuple) field12")
  public static class Field12 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 12);
    }
  }
  
  @Signature("(_ Tuple) field13")
  public static class Field13 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 13);
    }
  }
  
  @Signature("(_ Tuple) field14")
  public static class Field14 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 14);
    }
  }
  
  @Signature("(_ Tuple) field15")
  public static class Field15 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 15);
    }
  }
  
  @Signature("(_ Tuple) field16")
  public static class Field16 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 16);
    }
  }
  
  @Signature("(_ Tuple) field17")
  public static class Field17 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 17);
    }
  }
  
  @Signature("(_ Tuple) field18")
  public static class Field18 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 18);
    }
  }
  
  @Signature("(_ Tuple) field19")
  public static class Field19 implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return safeGetField(interpreter, arg, 19);
    }
  }
  
  private static Obj safeGetField(Interpreter interpreter, Obj tuple, int index) {
    // Negative indices count backwards from the end.
    if (index < 0) {
      index = tuple.asList().size() + index;
    }
    
    // Check the bounds.
    if ((index < 0) || (index >= tuple.asList().size())) {
      return interpreter.nothing();
    }
    
    Obj field = tuple.getTupleField(index);
    if (field != null) return field;
    
    return interpreter.nothing();
  }
}
