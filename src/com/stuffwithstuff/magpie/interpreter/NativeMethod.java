package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.util.Expect;

// TODO(bob): This contains the old way of defining native methods. Eventually,
// everything in here should be moved into builtin, which is the new hotness.
// Most stuff should move over cleanly. The only tricky ones are the parametric
// native methods like ClassFieldGetter where each instance of the NativeMethod
// has actual different state. The builtin stuff doesn't handle that yet.

public abstract class NativeMethod implements Callable {
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  public static FunctionType fn(String paramTypeName, String returnType) {
    return fn(Expr.name(paramTypeName), returnType);
  }
  
  public static FunctionType fn(Expr paramType, String returnType) {
    return fn(paramType, Expr.name(returnType));
  }
  
  public static FunctionType fn(String paramType, Expr returnType) {
    return fn(Expr.name(paramType), returnType);
  }
  
  public static FunctionType fn(Expr paramType, Expr returnType) {
    List<String> paramNames = new ArrayList<String>();
    // If there is a param (or multiple), give it a name.
    boolean isNothing =((paramType instanceof MessageExpr) &&
        ((MessageExpr)paramType).getName().equals("Nothing"));
    
    if (!isNothing) {
      paramNames.add("___");
    }
    
    return new FunctionType(paramNames, paramType, returnType);
  }

  // TODO(bob): Many of these just use dynamic in their type signature. Would be
  // good to change to something more specific when possible.

  // Class methods:

  public static class ClassDefineConstructor extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj method = (FnObj)arg;
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addConstructor(method);
      
      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn("Dynamic", "Nothing"); }
  }
  
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
  
  public static class ClassDeclareField extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj type = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;

      // Add a getter.
      classObj.addMethod(name,
          new NativeMethod.ClassFieldGetter(name, type.getFunction().getBody()));
      
      // Add a setter.
      classObj.addMethod(Identifiers.makeSetter(name),
          new NativeMethod.ClassFieldSetter(name, type.getFunction().getBody()));

      return interpreter.nothing();
    }
    
    public FunctionType getType() { return fn(Expr.tuple(
        Expr.name("String"), Expr.name("Dynamic")), "Nothing"); }
  }
  
  public static class ClassDefineField extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj initializer = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.defineField(name, initializer.getFunction().getBody());
      
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
  
  /**
   * Creates a new class.
   */
  public static class ClassNewClass extends NativeMethod {
    public ClassNewClass() {
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      
      // Get the name of the class.
      String name = arg.asString();
      
      // Create the metaclass. This will hold shared methods on the class.
      ClassObj metaclass = new ClassObj(interpreter.getMetaclass(),
          name + "Class", interpreter.getMetaclass());
      
      // Add the constructor method.
      metaclass.addMethod(Identifiers.NEW, new NativeMethod.ClassNew(name));

      // TODO(bob): Get rid of this.
      // Define a method to cheat the type-checker.
      metaclass.addMethod("unsafeCast", new NativeMethod.ClassUnsafeCast(name));

      // Create the class object itself. This will hold the instance methods for
      // objects of the class.
      ClassObj classObj = new ClassObj(metaclass, name,
          interpreter.getObjectType());
      
      return classObj;
    }
    
    public FunctionType getType() { return fn("String", "Class"); }
  }
  
  /**
   * Constructs a new instance of a class. This corresponds to 'Foo new', not
   * 'Class new' like above.
   */
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
      
      ClassObj thisClass = (ClassObj)thisObj;
      Callable method = thisClass.findMethod(name);
      
      if (method == null) {
        return interpreter.nothing();
      }
      
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
}
