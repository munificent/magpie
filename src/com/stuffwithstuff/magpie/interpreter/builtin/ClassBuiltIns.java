package com.stuffwithstuff.magpie.interpreter.builtin;

public class ClassBuiltIns {
  /*
  // TODO(bob): Get rid of type.
  @Signature("defineField(name String, initializer ->)")
  public static class DefineField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj initializer = arg.getTupleField(1).asFn();
      
      // Define the field itself.
      ClassObj classObj = thisObj.asClass();
      classObj.getFieldDefinitions().put(name,
          new Field(initializer.getFunction(), null));
      
      // Add a getter.
      classObj.getMembers().defineGetter(name, new FieldGetter(name));
      
      // Add a setter.
      classObj.getMembers().defineSetter(name, new FieldSetter(name));
  
      return interpreter.nothing();
    }
  }
    
  @Signature("defineMethod(name String, body ->)")
  public static class DefineMethod implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = arg.getTupleField(1).asFn();
      
      ClassObj classObj = thisObj.asClass();
      classObj.getMembers().defineMethod(name,
          method.getCallable().bindTo(classObj));
      
      return interpreter.nothing();
    }
  }
  
  @Signature("defineGetter(name String, body ->)")
  public static class DefineGetter implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj body = arg.getTupleField(1).asFn();
      
      ClassObj classObj = thisObj.asClass();
      classObj.getMembers().defineGetter(name,
          body.getCallable().bindTo(classObj));
      
      return interpreter.nothing();
    }
  }
  
  @Signature("defineSetter(name String, body ->)")
  public static class DefineSetter implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj body = arg.getTupleField(1).asFn();
      
      ClassObj classObj = thisObj.asClass();
      classObj.getMembers().defineSetter(name,
          body.getCallable().bindTo(classObj));
      
      return interpreter.nothing();
    }
  }
  
  @Getter("parents List(Class)")
  public static class Parents implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = thisObj.asClass();
      
      return interpreter.createArray(classObj.getParents());
    }
  }

  @Getter("name String")
  public static class GetName implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = thisObj.asClass();
      
      return interpreter.createString(classObj.getName());
    }
  }
  
  @Shared
  @Signature("new(name String -> Class)")
  public static class New implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // Get the name of the class.
      String name = arg.asString();
      
      return interpreter.createClass(name);
    }
  }
  
  @Shared
  @Signature("subclass?(derived, base -> Bool)")
  public static class Subclass implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // Make sure the arguments are classes.
      if (!arg.getTupleField(0).isClass()) return interpreter.createBool(false);
      if (!arg.getTupleField(1).isClass()) return interpreter.createBool(false);
      
      ClassObj derived = arg.getTupleField(0).asClass();
      ClassObj base = arg.getTupleField(1).asClass();
      
      return interpreter.createBool(derived.isSubclassOf(base));
    }
  }
  */
}
