package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.reflect.Method;

import com.stuffwithstuff.magpie.StringCharacterReader;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class BuiltIns {
  @SuppressWarnings("unchecked")
  public static void register(Class javaClass, ClassObj magpieClass)
  {
    for (Method method : javaClass.getDeclaredMethods()) {
      Signature signature = method.getAnnotation(Signature.class);
      if (signature != null) {
        registerMethod(magpieClass, method, signature.value());
      }
      
      Getter getter = method.getAnnotation(Getter.class);
      if (getter != null) {
        registerGetter(magpieClass, method, getter.value());
      }
    }
  }
  
  private static void registerMethod(ClassObj classObj, Method method,
      String signature) {
    try {
      Pair<String, FunctionType> parsed = parseSignature(signature);
      String methodName = parsed.getKey();
      FunctionType type = parsed.getValue();
      
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

  private static void registerGetter(ClassObj classObj, Method method,
      String signature) {
    try {
      Pair<String, FunctionType> parsed = parseSignature(signature);
      String methodName = parsed.getKey();
      FunctionType type = parsed.getValue();
      
      // See if it's shared.
      boolean isShared = method.getAnnotation(Shared.class) != null;
      
      // Define the getter.
      BuiltIn builtIn = new BuiltIn(type, method);
      if (isShared) {
        classObj.getClassObj().defineGetter(methodName, builtIn);
      } else {
        classObj.defineGetter(methodName, builtIn);
      }
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static Pair<String, FunctionType> parseSignature(String signature) {
    try {
      // Process the annotation to get the method's Magpie name and type
      // signature.
      Lexer lexer = new Lexer("", new StringCharacterReader(signature));
      MagpieParser parser = new MagpieParser(lexer);
      String name = parser.parseFunctionName();
      FunctionType type = parser.parseFunctionType();
      
      return new Pair<String, FunctionType>(name, type);
    } catch (ParseException e) {
      // TODO(bob): Hack. Better error handling.
      System.out.println("Could not parse built-in signature \"" +
          signature + "\".");
    }
    
    return null;
  }
}
