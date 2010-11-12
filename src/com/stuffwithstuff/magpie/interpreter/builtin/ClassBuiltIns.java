package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ClassBuiltIns {
  @Signature("declareField(name String, type ->)")
  public static class DeclareField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj type = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
  
      // Add a getter.
      classObj.defineGetter(name,
          new FieldGetter(name, type.getFunction().getFunction().getBody()));
      
      // Add a setter.
      classObj.defineSetter(name,
          new FieldSetter(name, type.getFunction().getFunction().getBody()));
  
      return interpreter.nothing();
    }
  }
  
  @Signature("defineConstructor(body ->)")
  public static class DefineConstructor implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj method = (FnObj)arg;
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addConstructor(method.getCallable());
      
      return interpreter.nothing();
    }
  }
  
  @Signature("defineField(name String, type, initializer ->)")
  public static class DefineField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj type = (FnObj)arg.getTupleField(1);
      FnObj initializer = (FnObj)arg.getTupleField(2);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.defineField(name, initializer);
  
      // Add a getter.
      classObj.defineGetter(name,
          new FieldGetter(name, type.getFunction().getFunction().getBody()));
      
      // Add a setter.
      classObj.defineSetter(name,
          new FieldSetter(name, type.getFunction().getFunction().getBody()));
  
      return interpreter.nothing();
    }
  }
    
  @Signature("defineMethod(name String, body ->)")
  public static class DefineMethod implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addMethod(name, method.getCallable());
      
      return interpreter.nothing();
    }
  }
  
  @Signature("defineGetter(name String, body ->)")
  public static class DefineGetter implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj body = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.defineGetter(name, body.getCallable());
      
      return interpreter.nothing();
    }
  }
  
  @Signature("defineSetter(name String, body ->)")
  public static class DefineSetter implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj body = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.defineSetter(name, body.getCallable());
      
      return interpreter.nothing();
    }
  }
  
  @Signature("getMemberType(name String -> Type | Nothing)")
  public static class GetMemberType implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      ClassObj thisClass = (ClassObj)thisObj;
      
      // Look for a getter.
      Callable getter = thisClass.findGetter(name);
      if (getter != null) {
        return interpreter.evaluateCallableType(getter, true);
      }
      
      // Look for a method.
      Callable method = thisClass.findMethod(name);
      if (method != null) {
        return interpreter.evaluateCallableType(method, false);
      }
  
      // Member not found.
      return interpreter.nothing();
    }
  }
  
  @Signature("getSetterType(name String -> Type | Nothing)")
  public static class GetSetterType implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      ClassObj thisClass = (ClassObj)thisObj;
      
      // Look for a setter.
      Callable setter = thisClass.findSetter(name);
      if (setter != null) {
        return interpreter.evaluateCallableType(setter, true);
      }
  
      // Setter not found.
      return interpreter.nothing();
    }
  }
  
  @Getter("name(-> String)")
  public static class Name implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      return interpreter.createString(classObj.getName());
    }
  }
    
  @Shared
  @Signature("new(name String -> Class)")
  public static class New implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // Get the name of the class.
      String name = arg.asString();
      
      // Create the metaclass. This will hold shared methods on the class.
      ClassObj metaclass = new ClassObj(interpreter.getMetaclass(),
          name + "Class", interpreter.getMetaclass());
      
      // Add the constructor method.
      metaclass.addMethod(Identifiers.NEW, new ClassNew(name));
  
      // Create the class object itself. This will hold the instance methods for
      // objects of the class.
      ClassObj classObj = new ClassObj(metaclass, name,
          interpreter.getObjectType());
      
      return classObj;
    }
  }
  
  @Getter("parent(-> Class)")
  public static class Parent implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      ClassObj parent = classObj.getParent();
      
      // If a class has no parent, its parent is implicitly Object.
      if (parent == null) return interpreter.getObjectType();
      
      return parent;
    }
  }
  
  @Setter("parent(parent Class -> Class)")
  public static class SetParent implements BuiltInCallable {
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
  }
}
