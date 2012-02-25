package com.stuffwithstuff.magpie.intrinsic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.PatternTester;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.StringReader;

/**
 * Defines built-in methods that are available as top-level global functions.
 */
public class IntrinsicMethods {
  @Def("currentTime()")
  public static class CurrentTime implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      // TODO(bob): Total hack to fit in an int.
      int time = (int) (System.currentTimeMillis() - 1289000000000L);
      return context.toObj(time);
    }
  }
  
  @Def("(is Function) call(arg)")
  @Doc("Invokes the given function.")
  public static class Call implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FnObj function = left.asFn();
      Obj fnArg = right;
      
      // Make sure the argument matches the function's pattern.
      Callable callable = function.getCallable();
      if (!PatternTester.test(context, callable.getPattern(), fnArg,
          callable.getClosure())) {
        throw context.error(Name.NO_METHOD_ERROR, "The argument \"" +
            context.getInterpreter().evaluateToString(fnArg) + "\" does not match the " +
            "function's pattern " + callable.getPattern());
      }

      return function.invoke(context, fnArg);
    }
  }

  // TODO(bob): More or less temporary.
  @Def("canParse(source is String)")
  public static class CanParse implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String source = right.asString();
      
      boolean canParse = true;
      
      try {
        MagpieParser parser = new MagpieParser(new StringReader("", source));

        while (true) {
          Expr expr = parser.parseStatement();
          if (expr == null) break;
        }
      } catch (ParseException e) {
        canParse = false;
      }
      
      return context.toObj(canParse);
    }
  }
  
  @Def("(this is Class) name")
  @Doc("Gets the name of the class.")
  public static class Class_Name implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.asClass().getName());
    }
  }

  @Def("(left) == (right)")
  @Doc("Returns true if left and right are the same object.")
  public static class Equals implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      // By default, "==" does reference equality.
      return context.toObj(left == right);
    }
  }

  @Def("(left is Record) ==(right is Record)")
  @Doc("Returns true if the two records have the same fields.")
  public static class Equals_Record implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      Map<String, Obj> fields = left.getFields();
      Set<String> compared = new HashSet<String>();
      
      // Make sure the right record has all of the left record's fields.
      for (Entry<String, Obj> entry : fields.entrySet()) {
        Obj rightField = right.getField(entry.getKey());
        if (rightField == null) return context.toObj(false);
        if (!context.getInterpreter().objectsEqual(
            entry.getValue(), rightField)) {
          return context.toObj(false);
        }
        compared.add(entry.getKey());
      }
      
      // Make sure the right record doesn't have any extra fields.
      for (String field : right.getFields().keySet()) {
        if (!compared.contains(field)) return context.toObj(false);
      }
      
      return context.toObj(true);
    }
  }
  
  @Def("(this) toString")
  @Doc("Returns a generic string representation of the object.")
  public static class ToString implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj("<" + left.getClassObj().getName() + ">");
    }
  }

  @Def("(is Record) toString")
  @Doc("Returns a string representation of the given record.")
  public static class Record_ToString implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.toString());
    }
  }

  @Def("(is Error) toString")
  @Doc("Returns a string representation of the error.")
  public static class Error_ToString implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      // If we've crammed a message in it (which internal errors do), then
      // show that.
      if (left.getValue() instanceof String) {
        String message = (String)left.getValue();
        return context.toObj(message);
      }
      
      // Just do the default.
      return context.toObj("<" + left.getClassObj().getName() + ">");
    }
  }
}
