package com.stuffwithstuff.magpie.intrinsic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class StringMethods {  
  @Def("(is String)[index is Int]")
  @Doc("Gets the character at the given index in the string (as a string).")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String string = left.asString();
      int index = right.asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) index = string.length() + index;
      
      if ((index < 0) || (index >= string.length())) {
        context.error(Name.OUT_OF_BOUNDS_ERROR, "Index " + index +
            " is out of bounds [0, " + string.length() + "].");
      }
      
      return context.toObj(string.substring(index, index + 1));
    }
  }

  @Def("(is String) count")
  @Doc("Returns the number of characters in the string.")
  public static class Count implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asString().length());
    }
  }
  
  @Def("(is String) +(right is String)")
  @Doc("Concatenates the two strings.")
  public static class Add implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asString() + right.asString());
    }
  }
  
  @Def("(is String) ==(other is String)")
  @Doc("Returns true if the two strings are equivalent.")
  public static class Equals implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asString().equals(
          right.asString()));
    }
  }
  
  @Def("(left is String) compareTo(right is String)")
  @Doc("Returns -1 if left comes lexicographically before right, 1 if it\n" +
       "comes after, and 0 if the two are equal.")
  public static class CompareTo implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asString().compareTo(
          right.asString()));
    }
  }
  
  @Def("(haystack is String) indexOf(needle is String)")
  @Doc("Returns the index in haystack of the first occurrence of needle or\n" +
       "nothing if it is not found.")
  public static class IndexOf implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      int index = left.asString().indexOf(right.asString());
      if (index == -1) return context.nothing();
      return context.toObj(index);
    }
  }
  
  @Def("(haystack is String) lastIndexOf(needle is String)")
  @Doc("Returns the index in haystack of the last occurrence of needle or\n" +
       "nothing if it is not found.")
  public static class LastIndexOf implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      int index = left.asString().lastIndexOf(right.asString());
      if (index == -1) return context.nothing();
      return context.toObj(index);
    }
  }
  
  // TODO(bob): Need to make this line up with Regex replace/replaceAll.
  @Def("(string is String) replace(before is String, with: after is String)")
  @Doc("Replaces each instance of before with after.")
  public static class Replace implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String string = left.asString();
      String before = right.getField(0).asString();
      String after = right.getField("with").asString();
      
      // TODO(bob): Should just be replacing a string, not a pattern.
      return context.toObj(
          string.replaceAll(before, Matcher.quoteReplacement(after)));
    }
  }
  
  // TODO(bob): Make an indexer that takes a range, so you can do:
  // "some string"[2 to(4)]
  @Def("(string is String) substring(from: from is Int, to: to is Int)")
  @Doc("Returns a substring of this string.")
  public static class Substring_FromTo implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String string = left.asString();
      int from = right.getField("from").asInt();
      int to = right.getField("to").asInt();
      return context.toObj(string.substring(from, to + 1));
    }
  }
  
  @Def("(string is String) substring(from: from is Int)")
  @Doc("Returns the portion of the string starting at from, to the\n" +
       "end of the string.")
  public static class Substring_From implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String string = left.asString();
      int startIndex = right.getField("from").asInt();
      return context.toObj(string.substring(startIndex));
    }
  }
  
  @Def("(string is String) substring(from: from is Int, count: count is Int)")
  @Doc("Returns the substring starting at the given index and with the " +
       "given length.")
  public static class Substring_FromCount implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String string = left.asString();
      int startIndex = Indexable.validateIndex(context, string,
          right.getField("from").asInt());
      int count = right.getField("count").asInt();
      
      int endIndex = startIndex + count;
      if (endIndex > string.length()) {
        context.error(Name.OUT_OF_BOUNDS_ERROR, "Index " + endIndex +
          " is out of bounds [0, " + string.length() + "].");
      }
      
      return context.toObj(string.substring(startIndex, startIndex + count));
    }
  }
  
  @Def("(is String) split(separator is String)")
  @Doc("Splits the string at every occurrence of the given separator and\n" +
       "returns an array of the given substrings.")
  public static class Split implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String[] parts = left.asString().split(right.asString());
      List<Obj> elements = new ArrayList<Obj>();
      for (String part : parts) {
        elements.add(context.toObj(part));
      }
      return context.toArray(elements);
    }
  }
}
