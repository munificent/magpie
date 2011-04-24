package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Scope;
import com.stuffwithstuff.magpie.parser.DefParser;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class BuiltIns {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void register(Class javaClass, Scope scope) {

    for (Class innerClass : javaClass.getDeclaredClasses()) {
      Signature signature = (Signature) innerClass.getAnnotation(Signature.class);
      if (signature != null) {
        registerMethod(scope, innerClass, signature.value());
      }
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void registerMethod(Scope scope,
      Class innerClass, String signature) {
    try {
      Pair<String, Pattern> parsed = parseSignature(signature);

      String name = parsed.getKey();
      Pattern pattern = parsed.getValue();
      
      // Construct the method.
      Constructor ctor = innerClass.getConstructor();
      BuiltInCallable callable = (BuiltInCallable) ctor.newInstance();
      BuiltIn builtIn = new BuiltIn(pattern, callable, scope);
      
      // Register it.
      scope.define(name, builtIn);
      
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
  
  private static Pair<String, Pattern> parseSignature(String text) {
    try {
      // Process the annotation to get the method's Magpie name and type
      // signature.
      MagpieParser parser = MagpieParser.create(text);
      return DefParser.parseSignature(parser);
    } catch (ParseException e) {
      // TODO(bob): Hack. Better error handling.
      System.out.println("Could not parse built-in signature \"" +
          text + "\".");
    }
    
    return null;
  }
}
