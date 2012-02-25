package com.stuffwithstuff.magpie.intrinsic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.Method;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Scope;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class IntrinsicLoader {
  /**
   * Given the name of a JVM classfile, loads it, finds all of the intrinsic
   * methods it specifies, and defines them in the given scope.
   * 
   * @param className  Name of the classfile to load.
   * @param scope      Scope to define the methods in.
   * @return           True if successful.
   */
  public static boolean loadClass(String className, Scope scope) {
    try {
      ClassLoader classLoader = IntrinsicLoader.class.getClassLoader();
      @SuppressWarnings("rawtypes")
      Class javaClass = classLoader.loadClass(className);
      IntrinsicLoader.register(javaClass, scope);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void register(Class javaClass, Scope scope) {

    for (Class innerClass : javaClass.getDeclaredClasses()) {
      Def signature = (Def) innerClass.getAnnotation(Def.class);
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
      
      Constructor ctor = innerClass.getConstructor();
      Object instance = ctor.newInstance();
      
      // If an external method, wrap it.
      if (instance instanceof Method) {
        // Must be an external method.
        instance = new MethodWrapper((Method) instance);
      }
      
      // Look for documentation.
      String doc = "";
      Doc docAnnotation = (Doc) innerClass.getAnnotation(Doc.class);
      if (docAnnotation != null) {
        doc = docAnnotation.value();
      }
      
      Callable callable = new IntrinsicCallable(pattern, doc, (Intrinsic) instance, scope);
      
      // Register it.
      scope.define(name, callable);
      
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
      MagpieParser parser = new MagpieParser(text);
      return parser.parseSignature();
    } catch (ParseException e) {
      // TODO(bob): Hack. Better error handling.
      System.out.println("Could not parse built-in signature \"" +
          text + "\".");
    }
    
    return null;
  }
}
