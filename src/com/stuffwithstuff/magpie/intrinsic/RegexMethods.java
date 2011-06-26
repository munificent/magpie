package com.stuffwithstuff.magpie.intrinsic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class RegexMethods {
  
  @Def("_setClasses(== Regex, == MatchResult, == UnsupportedModifierError)")
  public static class SetClasses implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      sRegexClass = right.getField(0).asClass();
      sMatchClass = right.getField(1).asClass();
      sModifierErrorClass = right.getField(2).asClass();
      return context.nothing();
    }
    
  }

  @Def("regex(pattern is String, modifiers is String)")
  @Doc("Compiles the pattern into a regular expression using the provided " +
       "modifiers.\n\nModifiers are supplied as a string of flags. The " +
       "following modifier flags are supported:\n" +
       "  * i - Makes the regular expression case insensitive.\n" +
       "  * m - Makes the regular expression support multiline patterns " +
       "allowing ^ and $ to match before and after line separators.\n" +
       "  * s - Makes the dot operator in the pattern match all characters " +
       "including line separators\n")
  public static class Regex implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      int modifiers = extractModifiers(right.getField(1).asString(), context);
      Pattern pattern = Pattern.compile(
          right.getField(0).asString(), modifiers);
      return context.instantiate(sRegexClass, pattern);
    }

    private int extractModifiers(String modifierString, Context context) {
      int modifiers = 0;
      for(int i = 0; i < modifierString.length(); i++) {
        switch(modifierString.charAt(i)) {
        case 'i': modifiers |= Pattern.CASE_INSENSITIVE; break;
        case 'm': modifiers |= Pattern.MULTILINE; break;
        case 's': modifiers |= Pattern.DOTALL; break;
        default:
          throw createUnsupportedModifierError(
              context, modifierString.charAt(i));
        }
      }
      return modifiers;
    }

    private ErrorException createUnsupportedModifierError(Context context, 
        char flag) {
      String message = "'" + flag + "' is not a supported regular " +
          "expression modifier.";
      Obj error = context.getInterpreter().instantiate(sModifierErrorClass, 
          message);
      
      error.setValue(message);
      
      return new ErrorException(error);
    }
    
  }

  @Def("(this is String) find(regex is Regex)")
  @Doc("Returns a MatchResult for the first occurrence of the regular " +
       "expression in this String or nothing if it is not found.")
  public static class Find implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      Pattern pattern = (Pattern)right.getValue();
      Matcher matcher = pattern.matcher(left.asString());
      if(matcher.find())
        return newMatchResult(context, matcher);
      return context.nothing();
    }
    
  }
  
  @Def("(this is String) findAll(regex is Regex)")
  @Doc("Returns an array of all occurrences of the regular expression in " +
       "this String.")
  public static class FindAll implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      Pattern pattern = (Pattern)right.getValue();
      List<Obj> finds = new ArrayList<Obj>();
      Matcher matcher = pattern.matcher(left.asString());
      while(matcher.find()) {
        finds.add(newMatchResult(context, matcher));
      }
      return context.toArray(finds);
    }
    
  }
  
  private static Obj newMatchResult(Context context, Matcher matcher) {
    List<Obj> groups = new ArrayList<Obj>();
    for(int i = 0; i <= matcher.groupCount(); i++) {
      groups.add(context.toObj(matcher.group(i)));
    }
    
    Map<String, Obj> fields = new HashMap<String, Obj>();
    fields.put("start", context.toObj(matcher.start()));
    fields.put("finish", context.toObj(matcher.end()));
    fields.put("groups", context.toArray(groups));
    
    return context.getInterpreter().constructNewObject(context, sMatchClass, 
        context.toObj(fields));
  }
  
  private static ClassObj sRegexClass;
  private static ClassObj sMatchClass;
  private static ClassObj sModifierErrorClass;

}
