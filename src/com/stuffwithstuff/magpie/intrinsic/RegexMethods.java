package com.stuffwithstuff.magpie.intrinsic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class RegexMethods {
  
  @Def("_setClasses(== Regex, == MatchResult)")
  public static class SetClasses implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      sRegexClass = right.getField(0).asClass();
      sMatchClass = right.getField(1).asClass();
      return context.nothing();
    }
    
  }

  @Def("regex(pattern is String, modifiers is String)")
  @Doc("Compiles the pattern into a regular expression using the provided " +
  		"modifiers. The modifiers change the behavior of the regular " +
  		"expression to allow case insensitive (i), multiline (m), " +
  		"dot matches all (s), and whitespace ignoring (x) patterns.")
  public static class Regex implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      int modifiers = extractModifiers(right.getField(1).asString());
      Pattern pattern = Pattern.compile(right.getField(0).asString(), modifiers);
      return context.instantiate(sRegexClass, pattern);
    }

    private int extractModifiers(String modifierString) {
      int modifiers = 0;
      for(int i = 0; i < modifierString.length(); i++) {
        switch(modifierString.charAt(i)) {
        case 'i': modifiers &= Pattern.CASE_INSENSITIVE; break;
        case 'm': modifiers &= Pattern.MULTILINE; break;
        case 's': modifiers &= Pattern.DOTALL; break;
        case 'x': modifiers &= Pattern.COMMENTS; break;
        }
      }
      return modifiers;
    }
    
  }

  @Def("(this is String) find(regex is Regex)")
  @Doc("Finds the first occurrence of the regular expression in the string.")
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
  @Doc("Returns an iterable over all occurrences of the regex in the String.")
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

}
