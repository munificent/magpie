package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;

public abstract class NativeMethodObj extends Obj implements Invokable {
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  // Native methods:
  
  // Bool methods:
  
  public static class BoolNot extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
  }
  
  public static class BoolToString extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
  }
  
  // Class methods:
  
  public static class ClassAddMethod extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addInstanceMethod(name, method);
      
      return interpreter.nothing();
    }
  }
  
  public static class ClassAddSharedMethod extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addMethod(name, method);
      
      return interpreter.nothing();
    }
  }

  public static class ClassFieldGetter extends NativeMethodObj {
    public ClassFieldGetter(String name) {
      mName = name;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getField(mName);
    }
    
    private final String mName;
  }

  public static class ClassFieldSetter extends NativeMethodObj {
    public ClassFieldSetter(String name) {
      mName = name;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      thisObj.setField(mName, arg);
      return arg;
    }
    
    private final String mName;
  }
  
  // TODO(bob): This is pretty much temp.
  public static class ClassNew extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
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
        obj.setField(field.getKey(), value);
      }
      
      // Find and call the constructor (if any).
      Invokable constructor = classObj.findConstructor(arg);
      if (constructor != null) {
        constructor.invoke(interpreter, obj, arg);
      }
      
      return obj;
    }
  }
  
  // Int methods:
  
  public static class IntPlus extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
  }

  public static class IntMinus extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
  }
  
  public static class IntMultiply extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
  }
  
  public static class IntDivide extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
  }
  
  public static class IntEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
  }
  
  public static class IntNotEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
  }
  
  public static class IntLessThan extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
  }
  
  public static class IntGreaterThan extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
  }
  
  public static class IntLessThanOrEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
  }
  
  public static class IntGreaterThanOrEqual extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
  }
  
  public static class IntToString extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Integer.toString(thisObj.asInt()));
    }
  }

  // String methods:

  public static class StringPlus extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String left = thisObj.asString();
      String right = arg.asString();
      
      return interpreter.createString(left + right);
    }
  }

  public static class StringPrint extends NativeMethodObj {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(thisObj.asString());
      return interpreter.nothing();
    }
  }
}
