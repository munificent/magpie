package com.stuffwithstuff.magpie.interpreter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.ast.*;

public abstract class NativeMethod implements Invokable {
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  // TODO(bob): Many of these just use dynamic in their type signature. Would be
  // good to change to something more specific when possible.

  // Array methods:
  
  public static class ArrayCount extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      return interpreter.createInt(elements.size());
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Int"); }
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
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Dynamic"); }
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
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Dynamic"); }
  }
  
  public static class ArrayAdd extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      elements.add(arg);
      
      return interpreter.nothing();
    }
    
    public Expr getParamType() { return Expr.name("Object"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
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
    
    public Expr getParamType() { return Expr.name("Dynamic"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
  }
  
  public static class ArrayRemoveAt extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      return elements.remove(arg.asInt());
    }
    
    public Expr getParamType() { return Expr.name("Dynamic"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
  }
  
  public static class ArrayClear extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      elements.clear();
      return interpreter.nothing();
    }
    
    public Expr getParamType() { return Expr.name("Dynamic"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
  }
  
  // Bool methods:
  
  public static class BoolNot extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class BoolToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("String"); }
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
    
    // TODO(bob): Should be tuple.
    public Expr getParamType() { return Expr.name("Dynamic"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
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
    
    // TODO(bob): Should be tuple.
    public Expr getParamType() { return Expr.name("Dynamic"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
  }
  
  public static class ClassGetName extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      return interpreter.createString(classObj.getName());
    }

    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("String"); }
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

    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Class"); }
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

    public Expr getParamType() { return Expr.name("Class"); }
    public Expr getReturnType() { return Expr.name("Class"); }
  }

  public static class ClassFieldGetter extends NativeMethod {
    public ClassFieldGetter(String name, Expr type) {
      mName = name;
      mType = type;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      Obj value = thisObj.getField(mName);
      if (value == null) return interpreter.nothing();
      return value;
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return mType; }

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
    
    public Expr getParamType() { return mType; }
    public Expr getReturnType() { return mType; }

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
      Invokable constructor = classObj.getConstructor();
      if (constructor != null) {
        constructor.invoke(interpreter, obj, arg);
      }
      
      return obj;
    }
    
    // TODO(bob): Should be typed.
    public Expr getParamType() { return Expr.name("Dynamic"); }
    public Expr getReturnType() { return Expr.name(mClassName); }
    
    private final String mClassName;
  }
  
  // Function methods:
  
  public static class FunctionApply extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj function = (FnObj)thisObj;
      
      return function.invoke(interpreter, interpreter.nothing(), arg);
    }
    
    // TODO(bob): These are not correct.
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Int"); }
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
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }

  public static class IntPlus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }

  public static class IntMinus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }
  
  public static class IntMultiply extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }
  
  public static class IntDivide extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }
  
  public static class IntEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class IntNotEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class IntLessThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class IntGreaterThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class IntLessThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class IntGreaterThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
  }
  
  public static class IntToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Integer.toString(thisObj.asInt()));
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("String"); }
  }
  
  // Object methods:
  
  public static class ObjectGetType extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getClassObj();
    }

    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Class"); }
  }
  
  public static class ObjectEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(thisObj == arg);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("Bool"); }
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
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
  }

  public static class ObjectPrint extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(arg.asString());
      return interpreter.nothing();
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Nothing"); }
  }

  // String methods:

  public static class StringConcatenate extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String left = thisObj.asString();
      String right = arg.asString();
      
      return interpreter.createString(left + right);
    }
    
    public Expr getParamType() { return Expr.name("String"); }
    public Expr getReturnType() { return Expr.name("String"); }
  }

  public static class StringAt extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int index = arg.asInt();
      String c = thisObj.asString().substring(index, index + 1);
      return interpreter.createString(c);
    }
    
    public Expr getParamType() { return Expr.name("Int"); }
    public Expr getReturnType() { return Expr.name("String"); }
  }

  public static class StringCompare extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createInt(thisObj.asString().compareTo(arg.asString()));
    }
    
    public Expr getParamType() { return Expr.name("String"); }
    public Expr getReturnType() { return Expr.name("Int"); }
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
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }
  
  public static class StringCount extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createInt(thisObj.asString().length());
    }
    
    public Expr getParamType() { return Expr.name("Nothing"); }
    public Expr getReturnType() { return Expr.name("Int"); }
  }
}
