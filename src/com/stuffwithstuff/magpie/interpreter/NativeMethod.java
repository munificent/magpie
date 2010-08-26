package com.stuffwithstuff.magpie.interpreter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.util.Expect;

public abstract class NativeMethod implements Callable {
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  protected static FunctionType fn(String paramTypeName, String returnType) {
    return fn(Expr.name(paramTypeName), returnType);
  }
  
  protected static FunctionType fn(Expr paramType, String returnType) {
    return fn(paramType, Expr.name(returnType));
  }
  
  protected static FunctionType fn(String paramType, Expr returnType) {
    return fn(Expr.name(paramType), returnType);
  }
  
  protected static FunctionType fn(Expr paramType, Expr returnType) {
    return new FunctionType(new ArrayList<String>(), paramType, returnType);
  }

  // TODO(bob): Many of these just use dynamic in their type signature. Would be
  // good to change to something more specific when possible.

  // Array methods:
  
  public static class ArrayCount extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      return interpreter.createInt(elements.size());
    }
    
    public FunctionType getType() { return fn("Nothing", "Int"); }
  }
  
  public static class ArrayGetElement extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      
      int index = arg.asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) {
        index = elements.size() + index;
      }
      
      return elements.get(index);
    }
    
    public FunctionType getType() { return fn("Int", "Dynamic"); }
  }
  
  public static class ArraySetElement extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      
      int index = arg.getTupleField(0).asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) {
        index = elements.size() + index;
      }
      
      elements.set(index, arg.getTupleField(1));
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn(Expr.tuple(
        Expr.name("Int"), Expr.name("Dynamic")), "Dynamic"); }
  }
  
  public static class ArrayAdd extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      elements.add(arg);
      
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn("Object", "Nothing"); }
  }
  
  public static class ArrayInsert extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int index = arg.getTupleField(0).asInt();
      Obj value = arg.getTupleField(1);

      List<Obj> elements = thisObj.asArray();
      elements.add(index, value);
      
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn("Dynamic", "Nothing"); }
  }
  
  public static class ArrayRemoveAt extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      return elements.remove(arg.asInt());
    }
    
    public FunctionType getType() { return fn("Dynamic", "Nothing"); }
  }
  
  public static class ArrayClear extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      elements.clear();
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn("Nothing", "Nothing"); }
  }
  
  // Bool methods:
  
  public static class BoolNot extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
    
    public FunctionType getType() { return fn("Nothing", "Bool"); }
  }
  
  public static class BoolToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
    
    public FunctionType getType() { return fn("Nothing", "String"); }
  }
  
  // Class methods:
  
  public static class ClassDefineMethod extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addMethod(name, method);
      
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn(Expr.tuple(
        Expr.name("String"), Expr.name("Dynamic")), "Nothing"); }
  }
  
  public static class ClassDefineSharedMethod extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      ClassObj metaclass = classObj.getClassObj();
      metaclass.addMethod(name, method);

      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn(Expr.tuple(
        Expr.name("String"), Expr.name("Dynamic")), "Nothing"); }
  }
  
  public static class ClassGetName extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      return interpreter.createString(classObj.getName());
    }

    public FunctionType getType() { return fn("Nothing", "String"); }
  }
  
  public static class ClassGetParent extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      ClassObj parent = classObj.getParent();
      
      // If a class has no parent, its parent is implicitly Object.
      if (parent == null) return interpreter.getObjectType();
      
      return parent;
    }

    public FunctionType getType() { return fn("Nothing", "Class"); }
  }
  
  public static class ClassSetParent extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      if (!(arg instanceof ClassObj)) {
        interpreter.runtimeError(
            "Cannot assign %s as the base class for %s because it is not a class.",
            arg, thisObj);
        
        return interpreter.nothing();
      }
      
      classObj.setParent((ClassObj)arg);
      return arg;
    }

    public FunctionType getType() { return fn("Class", "Class"); }
  }

  public static class ClassFieldGetter extends NativeMethod {
    public ClassFieldGetter(String name, Expr type) {
      Expect.notEmpty(name);
      Expect.notNull(type);
      
      mName = name;
      mType = type;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      Obj value = thisObj.getField(mName);
      if (value == null) return interpreter.nothing();
      return value;
    }
    
    public FunctionType getType() { return fn("Nothing", mType); }

    private final String mName;
    private final Expr mType;
  }

  public static class ClassFieldSetter extends NativeMethod {
    public ClassFieldSetter(String name, Expr type) {
      mName = name;
      mType = type;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      thisObj.setField(mName, arg);
      return arg;
    }
    
    public FunctionType getType() { return fn(mType, mType); }

    private final String mName;
    private final Expr mType;
  }
  
  // TODO(bob): This is pretty much temp.
  public static class ClassNew extends NativeMethod {
    public ClassNew(String className) {
      mClassName = className;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      Obj obj = classObj.instantiate();
      
      // Create a fresh context for evaluating the field initializers so that
      // they can't erroneously access stuff around where the object is being
      // constructed.
      EvalContext fieldContext = interpreter.createTopLevelContext();
      
      // Initialize its fields.
      for (Entry<String, Expr> field : classObj.getFieldInitializers().entrySet()) {
        Obj value = interpreter.evaluate(field.getValue(), fieldContext);
        obj.setField(field.getKey(), value);
      }
      
      // Find and call the constructor (if any).
      Callable constructor = classObj.getConstructor();
      if (constructor != null) {
        constructor.invoke(interpreter, obj, arg);
      }
      
      return obj;
    }
    
    public FunctionType getType() { return fn("Dynamic", Expr.name(mClassName)); }
    
    private final String mClassName;
  }

  public static class ClassUnsafeCast extends NativeMethod {
    public ClassUnsafeCast(String className) {
      mClassName = className;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      if (arg.getClassObj() != thisObj) {
        interpreter.runtimeError(
            "Cannot assign %s as the base class for %s because it is not a class.",
            arg, thisObj);
        
        // TODO(bob): Should throw an exception. Returning nothing here violates
        // the return type.
        return interpreter.nothing();
      }
      
      // Just echo the argument back. The important part is tha the annotated
      // type has changed.
      return arg;
    }

    public FunctionType getType() { return fn("Dynamic", Expr.name(mClassName)); }
    
    private final String mClassName;
  }

  public static class ClassGetMethodType extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      // TODO(bob): Arg type is ignored since there is no overloading yet.
      
      if (name.equals("unsafeRemoveType")) {
        System.out.println("!!!");
      }
      
      ClassObj thisClass = (ClassObj)thisObj;
      Callable method = thisClass.findMethod(name);
      
      if (method == null) {
        return interpreter.nothing();
      }
      
      // Make sure the argument matches the parameter type.
      Obj paramType = interpreter.evaluateType(method.getType().getParamType());
      Obj returnType = interpreter.evaluateType(method.getType().getReturnType());
      return interpreter.createTuple(paramType, returnType);
    }

    // TODO(bob): Should eventually be (String, IType) -> (IType, IType) | Nothing
    public FunctionType getType() { return fn("Dynamic", "Dynamic"); }
  }
  
  // Function methods:
  
  public static class FunctionCall extends NativeMethod {
    public FunctionCall(FunctionType type) {
      mType = type;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj function = (FnObj)thisObj;
      
      return function.invoke(interpreter, interpreter.nothing(), arg);
    }
    
    public FunctionType getType() { return mType; }
    
    private final FunctionType mType;
  }

  public static class FunctionGetType extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj function = (FnObj)thisObj;
      
      return interpreter.evaluateFunctionType(function.getType());
    }
    
    public FunctionType getType() { return fn("Nothing", "FunctionType"); }
  }
  
  // Int methods:
  
  public static class IntParse extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String text = arg.asString();
      
      try {
        int value = Integer.parseInt(text);
        return interpreter.createInt(value);
      } catch (NumberFormatException ex) {
        return interpreter.nothing();
      }
    }
    
    public FunctionType getType() { return fn("Int", "Int"); }
  }

  public static class IntPlus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
    
    public FunctionType getType() { return fn("Int", "Int"); }
  }

  public static class IntMinus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
    
    public FunctionType getType() { return fn("Int", "Int"); }
  }
  
  public static class IntMultiply extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
    
    public FunctionType getType() { return fn("Int", "Int"); }
  }
  
  public static class IntDivide extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
    
    public FunctionType getType() { return fn("Int", "Int"); }
  }
  
  public static class IntEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
    
    public FunctionType getType() { return fn("Int", "Bool"); }
  }
  
  public static class IntNotEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
    
    public FunctionType getType() { return fn("Int", "Bool"); }
  }
  
  public static class IntLessThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
    
    public FunctionType getType() { return fn("Int", "Bool"); }
  }
  
  public static class IntGreaterThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
    
    public FunctionType getType() { return fn("Int", "Bool"); }
  }
  
  public static class IntLessThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
    
    public FunctionType getType() { return fn("Int", "Bool"); }
  }
  
  public static class IntGreaterThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
    
    public FunctionType getType() { return fn("Int", "Bool"); }
  }
  
  public static class IntToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Integer.toString(thisObj.asInt()));
    }
    
    public FunctionType getType() { return fn("Nothing", "String"); }
  }
  
  // Object methods:
  
  public static class ObjectGetType extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getClassObj();
    }

    public FunctionType getType() { return fn("Nothing", "Class"); }
  }
  
  public static class ObjectEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(thisObj == arg);
    }
    
    public FunctionType getType() { return fn("Object", "Bool"); }
  }

  public static class ObjectImport extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String currentDir = new File(interpreter.getCurrentScript()).getParent();
      String relativePath = arg.asString();
      File scriptFile = new File(currentDir, relativePath);
      
      try {
        Script script = Script.fromPath(scriptFile.getPath());
        script.execute(interpreter);
      } catch (IOException e) {
        throw new InterpreterException("Could not load script \"" + relativePath + "\".");
      }
      
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn("String", "Nothing"); }
  }

  public static class ObjectPrint extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(arg.asString());
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn("String", "Nothing"); }
  }

  // String methods:

  public static class StringConcatenate extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String left = thisObj.asString();
      String right = arg.asString();
      
      return interpreter.createString(left + right);
    }
    
    public FunctionType getType() { return fn("String", "String"); }
  }

  public static class StringAt extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int index = arg.asInt();
      String c = thisObj.asString().substring(index, index + 1);
      return interpreter.createString(c);
    }
    
    public FunctionType getType() { return fn("Int", "String"); }
  }

  public static class StringCompare extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createInt(thisObj.asString().compareTo(arg.asString()));
    }
    
    public FunctionType getType() { return fn("String", "Int"); }
  }

  public static class StringSubstring extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // TODO(bob): Hackish way to see if we have one or two arguments to this.
      if (arg.getTupleField(0) != null) {
        int startIndex = arg.getTupleField(0).asInt();
        int endIndex = arg.getTupleField(1).asInt();
        String substring = thisObj.asString().substring(startIndex, endIndex);
        return interpreter.createString(substring);
      } else {
        int startIndex = arg.asInt();
        String substring = thisObj.asString().substring(startIndex);
        return interpreter.createString(substring);
      }
    }
    
    public FunctionType getType() { return fn("Dynamic", "String"); }
  }
  
  public static class StringCount extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createInt(thisObj.asString().length());
    }
    
    public FunctionType getType() { return fn("Nothing", "Int"); }
  }
}
