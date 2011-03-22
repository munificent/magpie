package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.MagpieToJava;
import com.stuffwithstuff.magpie.interpreter.Field;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Member;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ClassBuiltIns {
  @Signature("declareField(name String, delegate? Bool, type ->)")
  public static class DeclareField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      boolean isDelegate = arg.getTupleField(1).asBool();
      Expr type = MagpieToJava.convertExpr(interpreter, arg.getTupleField(2));
      
      ClassObj classObj = thisObj.asClass();
      classObj.declareField(name, isDelegate, type);
  
      return interpreter.nothing();
    }
  }
  
  // TODO(bob): Get rid of type.
  @Signature("defineField(name String, delegate? Bool, type, initializer ->)")
  public static class DefineField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      boolean isDelegate = arg.getTupleField(1).asBool();
      Obj optionalType = arg.getTupleField(2);
      FnObj initializer = arg.getTupleField(3).asFn();
      
      // Define the field itself.
      ClassObj classObj = thisObj.asClass();
      classObj.getFieldDefinitions().put(name,
          new Field(isDelegate, initializer.getFunction(), null));
  
      // Determine if the field is implicitly typed (we have an initializer but
      // no type annotation) or explicitly.
      Expr expr;
      boolean isInitializer;
      if (optionalType == interpreter.nothing()) {
        // No type annotation, so use the initializer.
        expr = initializer.getFunction().getFunction().getBody();
        isInitializer = true;
      } else {
        // Have a type annotation.
        expr = MagpieToJava.convertExpr(interpreter, optionalType);
        isInitializer = false;
      }
      
      // Add a getter.
      classObj.getMembers().defineGetter(name,
          new FieldGetter(name, expr, isInitializer));
      
      // Add a setter.
      classObj.getMembers().defineSetter(name,
          new FieldSetter(name, expr, isInitializer));
  
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
  
  @Signature("getMemberType(name String -> Type)")
  public static class GetMemberType implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      ClassObj thisClass = (ClassObj)thisObj;
      
      // Look for a member.
      Member member = ClassObj.findMember(thisClass, null, null, name);
      if (member != null) {
        return member.evaluateType(interpreter);
      }
  
      // Member not found.
      return interpreter.getNothingClass();
    }
  }
  
  @Getter("mixins List(Class)")
  public static class Mixins implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = thisObj.asClass();
      
      return interpreter.createArray(classObj.getMixins());
    }
  }

  @Getter("name String")
  public static class GetName implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = thisObj.asClass();
      
      return interpreter.createString(classObj.getName());
    }
  }
  
  // TODO(bob): If mixins don't subtype, then this doesn't actually return the
  // right type (since the class of the object returned (a metaclass) isn't an
  // instance of Class, it just mixes it in). Make Class an interface?
  @Shared
  @Signature("new(name String -> Class)")
  public static class New implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // Get the name of the class.
      String name = arg.asString();
      
      return interpreter.createClass(name);
    }
  }
}
