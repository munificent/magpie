package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.stuffwithstuff.magpie.StringCharacterReader;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class BuiltIns {
  @SuppressWarnings("unchecked")
  public static void registerClass(Class javaClass, ClassObj magpieClass)
  {
    for (Class innerClass : javaClass.getDeclaredClasses()) {
      Signature signature = (Signature) innerClass.getAnnotation(Signature.class);
      if (signature != null) {
        registerMethod(magpieClass, innerClass, signature.value());
      }
      
      Getter getter = (Getter) innerClass.getAnnotation(Getter.class);
      if (getter != null) {
        registerGetter(magpieClass, innerClass, getter.value());
      }
      
      Setter setter = (Setter) innerClass.getAnnotation(Setter.class);
      if (setter != null) {
        registerSetter(magpieClass, innerClass, setter.value());
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public static void registerFunctions(Class javaClass,
      Interpreter interpreter) {
    for (Class innerClass : javaClass.getDeclaredClasses()) {
      Signature signature = (Signature) innerClass.getAnnotation(Signature.class);
      if (signature != null) {
        registerFunction(interpreter, innerClass, signature.value());
      }
    }
  }
  
  // TODO(bob): These are all almost identical. Refactor.
  
  @SuppressWarnings("unchecked")
  private static void registerFunction(Interpreter interpreter,
      Class innerClass, String signature) {
    try {
      Pair<String, FunctionType> parsed = parseSignature(signature);
      String functionName = parsed.getKey();
      FunctionType type = parsed.getValue();
      
      // Construct the object.
      Constructor ctor = innerClass.getConstructor();
      BuiltInCallable callable = (BuiltInCallable) ctor.newInstance();
      
      // Define the function.
      BuiltIn builtIn = new BuiltIn(type, callable);
      FnObj function = new FnObj(interpreter.getFunctionClass(),
          interpreter.nothing(), builtIn);
      interpreter.getGlobals().define(functionName, function);
      
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  @SuppressWarnings("unchecked")
  private static void registerMethod(ClassObj classObj, Class innerClass,
      String signature) {
    try {
      Pair<String, FunctionType> parsed = parseSignature(signature);
      String methodName = parsed.getKey();
      FunctionType type = parsed.getValue();
      
      // See if it's shared.
      boolean isShared = innerClass.getAnnotation(Shared.class) != null;
      
      // Construct the object.
      Constructor ctor = innerClass.getConstructor();
      BuiltInCallable callable = (BuiltInCallable) ctor.newInstance();
      
      // Define the method.
      BuiltIn builtIn = new BuiltIn(type, callable);
      if (isShared) {
        classObj.getClassObj().getMembers().defineMethod(methodName, builtIn);
      } else {
        classObj.getMembers().defineMethod(methodName, builtIn);
      }
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  private static void registerGetter(ClassObj classObj, Class innerClass,
      String signature) {
    try {
      Pair<String, Expr> parsed = parseGetter(signature);
      String methodName = parsed.getKey();
      Expr type = parsed.getValue();
      
      // See if it's shared.
      boolean isShared = innerClass.getAnnotation(Shared.class) != null;
      
      // Construct the object.
      Constructor ctor = innerClass.getConstructor();
      BuiltInCallable callable = (BuiltInCallable) ctor.newInstance();

      // Define the getter.
      BuiltIn builtIn = new BuiltIn(type, callable);
      if (isShared) {
        classObj.getClassObj().getMembers().defineGetter(methodName, builtIn);
      } else {
        classObj.getMembers().defineGetter(methodName, builtIn);
      }
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  private static void registerSetter(ClassObj classObj, Class innerClass,
      String signature) {
    try {
      Pair<String, FunctionType> parsed = parseSignature(signature);
      String methodName = parsed.getKey();
      FunctionType type = parsed.getValue();
      
      // See if it's shared.
      boolean isShared = innerClass.getAnnotation(Shared.class) != null;
      
      // Construct the object.
      Constructor ctor = innerClass.getConstructor();
      BuiltInCallable callable = (BuiltInCallable) ctor.newInstance();

      // Define the setter.
      BuiltIn builtIn = new BuiltIn(type, callable);
      if (isShared) {
        classObj.getClassObj().getMembers().defineSetter(methodName, builtIn);
      } else {
        classObj.getMembers().defineSetter(methodName, builtIn);
      }
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
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
  
  private static Pair<String, Expr> parseGetter(String signature) {
    try {
      // Process the annotation to get the method's Magpie name and type
      // signature.
      Lexer lexer = new Lexer("", new StringCharacterReader(signature));
      MagpieParser parser = new MagpieParser(lexer);
      String name = parser.parseFunctionName();
      Expr type = parser.parseTypeExpression();
      
      return new Pair<String, Expr>(name, type);
    } catch (ParseException e) {
      // TODO(bob): Hack. Better error handling.
      System.out.println("Could not parse built-in signature \"" +
          signature + "\".");
    }
    
    return null;
  }
}
