package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ClassBuiltIns {
  @Signature("declareField(name String, type ->)")
  public static Obj declareField(Interpreter interpreter, Obj thisObj, Obj arg) {
    String name = arg.getTupleField(0).asString();
    FnObj type = (FnObj)arg.getTupleField(1);
    
    ClassObj classObj = (ClassObj)thisObj;

    // Add a getter.
    classObj.addMethod(name,
        new FieldGetter(name, type.getFunction().getBody()));
    
    // Add a setter.
    classObj.addMethod(Identifiers.makeSetter(name),
        new FieldSetter(name, type.getFunction().getBody()));

    return interpreter.nothing();
  }
  
  @Signature("defineConstructor(body ->)")
  public static Obj defineConstructor(Interpreter interpreter, Obj thisObj, Obj arg) {
    FnObj method = (FnObj)arg;
    
    ClassObj classObj = (ClassObj)thisObj;
    classObj.addConstructor(method);
    
    return interpreter.nothing();
  }

  @Signature("defineField(name String, initializer ->)")
  public static Obj defineField(Interpreter interpreter, Obj thisObj, Obj arg) {
    String name = arg.getTupleField(0).asString();
    FnObj initializer = (FnObj)arg.getTupleField(1);
    
    ClassObj classObj = (ClassObj)thisObj;
    classObj.defineField(name, initializer.getFunction().getBody());
    
    return interpreter.nothing();
  }
  
  @Signature("defineMethod(name String, body ->)")
  public static Obj defineMethod(Interpreter interpreter, Obj thisObj, Obj arg) {
    String name = arg.getTupleField(0).asString();
    FnObj method = (FnObj)arg.getTupleField(1);
    
    ClassObj classObj = (ClassObj)thisObj;
    classObj.addMethod(name, method);
    
    return interpreter.nothing();
  }
  
  @Signature("getMethodType(name)")
  public static Obj getMethodType(Interpreter interpreter, Obj thisObj, Obj arg) {
    String name = arg.getTupleField(0).asString();
    // TODO(bob): Arg type is ignored since there is no overloading yet.
    
    ClassObj thisClass = (ClassObj)thisObj;
    Callable method = thisClass.findMethod(name);
    
    if (method == null) {
      return interpreter.nothing();
    }
    
    // TODO(bob): Hackish.
    // Figure out a context to evaluate the method's type signature in. If it's
    // a user-defined method we'll evaluate it the method's closure so that
    // outer static arguments are available. Otherwise, we'll assume it has no
    // outer scope and just evaluate it in a top-level context.
    EvalContext staticContext;
    if (method instanceof FnObj) {
      staticContext = new EvalContext(((FnObj)method).getClosure(),
          interpreter.nothing());
    } else {
      staticContext = interpreter.createTopLevelContext();
    }
    
    Obj paramType = interpreter.evaluate(method.getType().getParamType(),
        staticContext);
    Obj returnType = interpreter.evaluate(method.getType().getReturnType(),
        staticContext);
    return interpreter.createTuple(paramType, returnType);
  }
  
  @Signature("name(-> String)")
  public static Obj name(Interpreter interpreter, Obj thisObj, Obj arg) {
    ClassObj classObj = (ClassObj)thisObj;
    
    return interpreter.createString(classObj.getName());
  }
  
  @Shared
  @Signature("new(name String -> Class)")
  public static Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    // Get the name of the class.
    String name = arg.asString();
    
    // Create the metaclass. This will hold shared methods on the class.
    ClassObj metaclass = new ClassObj(interpreter.getMetaclass(),
        name + "Class", interpreter.getMetaclass());
    
    // Add the constructor method.
    metaclass.addMethod(Identifiers.NEW, new ClassNew(name));

    // TODO(bob): Get rid of this.
    // Define a method to cheat the type-checker.
    metaclass.addMethod("unsafeCast", new UnsafeCast(name));

    // Create the class object itself. This will hold the instance methods for
    // objects of the class.
    ClassObj classObj = new ClassObj(metaclass, name,
        interpreter.getObjectType());
    
    return classObj;
  }

  @Signature("parent(-> Class)")
  public static Obj parent(Interpreter interpreter, Obj thisObj, Obj arg) {
    ClassObj classObj = (ClassObj)thisObj;
    
    ClassObj parent = classObj.getParent();
    
    // If a class has no parent, its parent is implicitly Object.
    if (parent == null) return interpreter.getObjectType();
    
    return parent;
  }
  
  @Signature("parent=(parent Class -> Class)")
  public static Obj parent_eq_(Interpreter interpreter, Obj thisObj, Obj arg) {
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
