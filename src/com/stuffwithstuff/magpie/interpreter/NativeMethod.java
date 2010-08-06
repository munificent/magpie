package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.type.*;

public abstract class NativeMethod implements Invokable {
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  public abstract FunctionType getFunctionType();
  
  // TODO(bob): Many of these just use dynamic in their type signature. Would be
  // good to change to something more specific when possible.
  
  // Native methods:
  
  // Bool methods:
  
  public static class BoolNot extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.nothing(), TypeDecl.boolType());
    }
  }
  
  public static class BoolToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.nothing(), TypeDecl.stringType());
    }
  }
  
  // Class methods:
  
  public static class ClassAddMethod extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addInstanceMethod(name, method);
      
      return interpreter.nothing();
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(new TupleType(
          TypeDecl.stringType(), TypeDecl.functionType()),
          TypeDecl.nothing());
    }
  }
  
  public static class ClassAddSharedMethod extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addMethod(name, method);

      return interpreter.nothing();
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(new TupleType(
          TypeDecl.stringType(), TypeDecl.functionType()),
          TypeDecl.nothing());
    }
  }

  public static class ClassFieldGetter extends NativeMethod {
    public ClassFieldGetter(String name) {
      mName = name;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getField(mName);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.dynamic(), TypeDecl.dynamic());
    }

    private final String mName;
  }

  public static class ClassFieldSetter extends NativeMethod {
    public ClassFieldSetter(String name) {
      mName = name;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      thisObj.setField(mName, arg);
      return arg;
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.dynamic(), TypeDecl.dynamic());
    }

    private final String mName;
  }
  
  // TODO(bob): This is pretty much temp.
  public static class ClassNew extends NativeMethod {
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
      Invokable constructor = classObj.getConstructor();
      if (constructor != null) {
        constructor.invoke(interpreter, obj, arg);
      }
      
      return obj;
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.dynamic(), TypeDecl.dynamic());
    }
  }
  
  // Int methods:
  
  public static class IntPlus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.intType());
    }
  }

  public static class IntMinus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.intType());
    }
  }
  
  public static class IntMultiply extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.intType());
    }
  }
  
  public static class IntDivide extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.intType());
    }
  }
  
  public static class IntEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.boolType());
    }
  }
  
  public static class IntNotEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.boolType());
    }
  }
  
  public static class IntLessThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.boolType());
    }
  }
  
  public static class IntGreaterThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.boolType());
    }
  }
  
  public static class IntLessThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.boolType());
    }
  }
  
  public static class IntGreaterThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.boolType());
    }
  }
  
  public static class IntToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Integer.toString(thisObj.asInt()));
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.nothing(), TypeDecl.stringType());
    }
  }

  // String methods:

  public static class StringPlus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String left = thisObj.asString();
      String right = arg.asString();
      
      return interpreter.createString(left + right);
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.stringType(), TypeDecl.stringType());
    }
  }

  public static class StringPrint extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(thisObj.asString());
      return interpreter.nothing();
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.nothing(), TypeDecl.nothing());
    }
  }
  
  // Tuple methods:

  public static class TupleGetField extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getTupleField(arg.asInt());
    }
    
    @Override
    public FunctionType getFunctionType() {
      return new FunctionType(TypeDecl.intType(), TypeDecl.dynamic());
    }
  }
}
