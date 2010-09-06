package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.reflect.Method;

import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;

public abstract class BuiltIns {
  @SuppressWarnings("unchecked")
  public static void register(Class javaClass, ClassObj magpieClass)
  {
    for (Method method : javaClass.getDeclaredMethods()) {
      Signature signature = method.getAnnotation(Signature.class);
      if (signature != null) {
        register(magpieClass, method, signature);
      }
    }
  }
  
  private static void register(ClassObj classObj, Method method,
      Signature signature) {
    try {
      // Process the annotation to get the method's Magpie name and type
      // signature.
      Lexer lexer = new Lexer("", signature.value());
      MagpieParser parser = new MagpieParser(lexer);
      String methodName = parser.parseFunctionName();
      FunctionType type = parser.parseFunctionType();
      
      // See if it's shared.
      boolean isShared = method.getAnnotation(Shared.class) != null;
      
      // Define the method.
      BuiltIn builtIn = new BuiltIn(type, method);
      if (isShared) {
        classObj.getClassObj().addMethod(methodName, builtIn);
      } else {
        classObj.addMethod(methodName, builtIn);
      }
      
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
