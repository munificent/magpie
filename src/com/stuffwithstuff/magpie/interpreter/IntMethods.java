package com.stuffwithstuff.magpie.interpreter;

/**
 * Built-in methods on ints.
 */
public class IntMethods {
  public static Invokable operatorPlus() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createInt(left + right);
      }
    };
  }

  public static Invokable operatorMinus() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createInt(left - right);
      }
    };
  }

  public static Invokable operatorMultiply() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createInt(left * right);
      }
    };
  }

  public static Invokable operatorDivide() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createInt(left / right);
      }
    };
  }

  public static Invokable operatorEqual() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createBool(left == right);
      }
    };
  }

  public static Invokable operatorNotEqual() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createBool(left != right);
      }
    };
  }
  
  public static Invokable operatorLessThan() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createBool(left < right);
      }
    };
  }

  public static Invokable operatorGreaterThan() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createBool(left > right);
      }
    };
  }

  public static Invokable operatorLessThanOrEqual() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createBool(left <= right);
      }
    };
  }

  public static Invokable operatorGreaterThanOrEqual() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int left = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        int right = ((Integer)arg.getPrimitiveValue()).intValue();
        
        return interpreter.createBool(left >= right);
      }
    };
  }

  public static Invokable toStringMethod() {
    return new Invokable() {
      public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
        int value = ((Integer)context.getThis().getPrimitiveValue()).intValue();
        return interpreter.createString(Integer.toString(value));
      }
    };
  }
}
