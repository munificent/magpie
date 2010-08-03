package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;

public abstract class NativeMethodObj extends Obj implements Invokable {
  public abstract Obj invoke(Interpreter interpreter, EvalContext context, Obj arg);
  
  // Native methods:
  
  // Bool methods:
  
  public static class BoolNot extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      return interpreter.createBool(!context.getThis().asBool());
    }
  }
  
  public static class BoolToString extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      return interpreter.createString(Boolean.toString(context.getThis().asBool()));
    }
  }
  
  // Class methods:
  
  public static class ClassAddMethod extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      String name = arg.getTupleField(0).asString();
      Obj method = arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)context.getThis();
      classObj.addInstanceMember(name, method);
      
      return interpreter.nothing();
    }
  }
  
  public static class ClassAddSharedMethod extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      String name = arg.getTupleField(0).asString();
      Obj method = arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)context.getThis();
      classObj.add(name, method);
      
      return interpreter.nothing();
    }
  }
  
  // TODO(bob): This is pretty much temp.
  public static class ClassNew extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      ClassObj classObj = (ClassObj)context.getThis();
      
      // Instantiate the object.
      Obj obj = new Obj(classObj);
      
      // Create a fresh context for evaluating the field initializers so that
      // they can't erroneously access stuff around where the object is being
      // constructed.
      EvalContext fieldContext = EvalContext.topLevel(
          interpreter.getGlobals(), interpreter.nothing());
      
      // Initialize its fields.
      for (Entry<String, Expr> field : classObj.getFieldInitializers().entrySet()) {
        Obj value = interpreter.evaluate(field.getValue(), fieldContext);
        obj.add(field.getKey(), value);
      }
      
      // TODO(bob): Need to call constructor here and pass in arg.
      
      return obj;
    }
  }
  
  // Int methods:
  
  public static class IntPlus extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
  }

  public static class IntMinus extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
  }
  
  public static class IntMultiply extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
  }
  
  public static class IntDivide extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
  }
  
  public static class IntEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
  }
  
  public static class IntNotEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
  }
  
  public static class IntLessThan extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
  }
  
  public static class IntGreaterThan extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
  }
  
  public static class IntLessThanOrEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
  }
  
  public static class IntGreaterThanOrEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      int left = context.getThis().asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
  }
  
  public static class IntToString extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      return interpreter.createString(Integer.toString(context.getThis().asInt()));
    }
  }

  // String methods:

  public static class StringPlus extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      String left = context.getThis().asString();
      String right = arg.asString();
      
      return interpreter.createString(left + right);
    }
  }

  public static class StringPrint extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, EvalContext context, Obj arg) {
      interpreter.print(context.getThis().asString());
      return interpreter.nothing();
    }
  }
}
