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

  @Def("(this is String) asRegex")
  @Doc("Compiles this string into a regular expression.")
  public static class AsRegex implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      String stringPattern = left.asString();
      Pattern pattern = Pattern.compile(stringPattern);
      return context.instantiate(sRegexClass, pattern);
    }
    
  }
  
  @Def("(this is String) contains(regex is Regex)")
  @Doc("Returns true if the String contains the regular expression.")
  public static class Contains implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      Pattern pattern = (Pattern)right.getValue();
      return context.toObj(pattern.matcher(left.asString()).find());
    }
    
  }
  
  @Def("(this is String) find(regex is Regex)")
  @Doc("Returns an iterable over all occurrences of the regex in the String.")
  public static class Find implements Intrinsic {

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
  
  @Def("(this is String) matches(regex is Regex)")
  @Doc("Returns a MatchResult for regex against this if the regular expression matches.")
  public static class Matches implements Intrinsic {

    @Override
    public Obj invoke(Context context, Obj left, Obj right) {
      Pattern pattern = (Pattern)right.getValue();

      Matcher matcher = pattern.matcher(left.asString());
      
      if(!matcher.matches()) {
        return context.nothing();
      }
      
      return newMatchResult(context, matcher);
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
    
    return context.getInterpreter().constructNewObject(context, sMatchClass, context.toObj(fields));
    
  }
  
  private static ClassObj sRegexClass;
  private static ClassObj sMatchClass;

}
