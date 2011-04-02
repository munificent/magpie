package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.stuffwithstuff.magpie.StringCharacterReader;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.MultimethodObj;
import com.stuffwithstuff.magpie.parser.DefParser;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.util.Pair;

public abstract class BuiltIns {
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void registerMethods(Class javaClass,
      Interpreter interpreter) {

    for (Class innerClass : javaClass.getDeclaredClasses()) {
      Signature signature = (Signature) innerClass.getAnnotation(Signature.class);
      if (signature != null) {
        registerMethod(interpreter, innerClass, signature.value());
      }
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void registerMethod(Interpreter interpreter,
      Class innerClass, String signature) {
    try {
      Pair<String, Pattern> parsed = parseSignature(signature);

      String name = parsed.getKey();
      Pattern pattern = parsed.getValue();
      
      // Construct the method.
      Constructor ctor = innerClass.getConstructor();
      BuiltInCallable callable = (BuiltInCallable) ctor.newInstance();
      BuiltIn builtIn = new BuiltIn(pattern, callable);
      
      // Register it.
      MultimethodObj multimethod = interpreter.getMultimethod(name);
      multimethod.addMethod(builtIn);
      
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
      Lexer lexer = new Lexer("", new StringCharacterReader(text));
      MagpieParser parser = new MagpieParser(lexer);
      return DefParser.parseSignature(parser);
    } catch (ParseException e) {
      // TODO(bob): Hack. Better error handling.
      System.out.println("Could not parse built-in signature \"" +
          text + "\".");
    }
    
    return null;
  }
}
