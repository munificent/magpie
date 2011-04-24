package com.stuffwithstuff.magpie.interpreter.builtin;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Multimethod;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ReflectBuiltIns {
  @Signature("(_) class")
  public static class Class_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return arg.getClassObj();
    }
  }

  @Signature("(_) is?(class Class)")
  public static class Is implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(arg.getField(0).getClassObj().isSubclassOf(
          arg.getField(1).asClass()));
    }
  }
  
  @Signature("(_) sameAs?(other)")
  public static class Same implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      return interpreter.createBool(
          arg.getField(0) == arg.getField(1));
    }
  }
  
  @Signature("docMethod(methodName String)")
  public static class DocMethod implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      String name = arg.getField(1).asString();
      
      // TODO(bob): Hackish, but works.
      Multimethod multimethod = interpreter.getCurrentModule().getScope()
          .lookUpMultimethod(name);
      
      if (multimethod == null) {
        interpreter.print("Couldn't find a method named \"" + name + "\".\n");
      } else {
        for (Callable method : multimethod.getMethods()) {
          Pattern pattern = method.getPattern();
          if (pattern instanceof RecordPattern) {
            RecordPattern record = (RecordPattern)pattern;
            interpreter.print(String.format("(%s) %s(%s)\n",
                record.getFields().get(Name.getTupleField(0)),
                name,
                record.getFields().get(Name.getTupleField(1))));
          } else {
            interpreter.print(String.format("%s %s\n",
                pattern, name));
          }
          
          interpreter.print("| " + method.getDoc().replace("\n", "\n| ") + "\n");
        }
      }
      
      return interpreter.nothing();
    }
  }
  
  @Signature("(_ Class) doc")
  public static class ClassDoc implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      ClassObj classObj = arg.getField(1).asClass();
      
      interpreter.print(classObj.getName() + "\n");
      if (classObj.getDoc().length() > 0) {
        interpreter.print("| " + classObj.getDoc().replace("\n", "\n| ") + "\n");
      } else {
        interpreter.print("| <no doc>\n");
      }
      return interpreter.nothing();
    }
  }
}
