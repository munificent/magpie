package com.stuffwithstuff.magpie.intrinsic;

import java.util.Map.Entry;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.ast.pattern.RecordPattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Multimethod;
import com.stuffwithstuff.magpie.interpreter.DocBuilder;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.PatternTester;
import com.stuffwithstuff.magpie.interpreter.Scope;

public class ReflectionMethods {
  @Def("(this) class")
  @Doc("Gets the class of the given object.")
  public static class Class_ implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return left.getClassObj();
    }
  }

  // TODO(bob): Come up with better name.
  @Def("(this) isa(class is Class)")
  @Doc("Returns true if this is an instance of the given class or one of\n" +
       "its subclasses.")
  public static class Is implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(left.getClassObj().isSubclassOf(
          right.asClass()));
    }
  }
  
  @Def("(this) sameAs(that)")
  @Doc("Returns true if this and that are the same object.")
  public static class Same implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(
          left == right);
    }
  }
  
  @Def("showDoc(method is String)")
  @Doc("Displays the documentation for the multimethod with the given name.")
  public static class DocMethod implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String name = right.asString();
      
      // TODO(bob): Hackish, but works.
      Multimethod multimethod = context.getModule().getScope()
          .lookUpMultimethod(name);
            
      if (multimethod == null) {
        System.out.println(
            "Couldn't find a method named \"" + name + "\".\n");
      } else {
        // showDoc
        // Displays the documentation for the given argument.
        //
        // showDoc(method is String)
        //   Displays the documentation for the multimethod with the given name.
        // showDoc(class is Class)
        //   Displays the documentation for the given class.
        System.out.println(new DocBuilder().append(name, multimethod).toString());
      }
      
      return context.nothing();
    }
  }
  
  @Def("showDoc(class is Class)")
  @Doc("Displays the documentation for the given class.")
  public static class ClassDoc implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      ClassObj classObj = right.asClass();
      
      StringBuilder builder = new StringBuilder();
      builder.append(classObj.getName() + "\n");
      if (classObj.getDoc().length() > 0) {
        builder.append("| " + classObj.getDoc().replace("\n", "\n| ") + "\n");
      } else {
        builder.append("| <no doc>\n");
      }
      
      System.out.println(builder.toString());
      
      return context.nothing();
    }
  }

  private static abstract class Methods implements Intrinsic {
    public Obj invoke(final Context context, Obj left, Obj right) {
     
      final Obj matchingValue = getMatchingValueFromArgs(context, left, right);

      DocBuilder docBuilder = new DocBuilder() {

        @Override
        protected boolean shouldDisplayMethod(Callable callable) {
          return isMatchingMethod(matchingValue, context, callable);
        }
      };

      for(Entry<String, Multimethod> multimethod : findMultimethods(context, matchingValue)) {
        docBuilder.append(multimethod.getKey(), multimethod.getValue());
        docBuilder.append("\n");
      }
      System.out.println(docBuilder.toString());
      
      return context.nothing();
    }

    protected Set<Entry<String, Multimethod>> findMultimethods(Context context, Obj matchingValue) {
      Map<String, Multimethod> matching = new TreeMap<String, Multimethod>();

      Scope scope = context.getModule().getScope();
      while (scope != null) {
        for(Entry<String, Multimethod> multimethod : scope.getMultimethods().entrySet()) {
          for(Callable method : multimethod.getValue().getMethods()) {
            if(isMatchingMethod(matchingValue, context, method)) {
              matching.put(multimethod.getKey(), multimethod.getValue());
              break;
            }
          }  
        }
        scope = scope.getParent();
      }
 
      return matching.entrySet();
    }
  
    protected abstract Obj getMatchingValueFromArgs(Context context, Obj left, Obj right);
    protected abstract boolean isMatchingMethod(Obj value, Context context, Callable method);
  }

  @Def("(this) showMethods")
  @Doc("Displays the multimethods that can be called with this object on the left.")
  public static class MethodsFromLeft extends Methods {

    @Override
    protected Obj getMatchingValueFromArgs(Context context, Obj left, Obj right) {
      return left;
    }

    @Override
    public boolean isMatchingMethod(Obj value, Context context, Callable method) {
      Pattern leftParam = ((RecordPattern)method.getPattern()).getFields().get(Name.getTupleField(0));
      return PatternTester.test(context, leftParam, value, method.getClosure());
    }
  }  

  @Def("(this) showMethods(_)")
  @Doc("Displays the multimethods that can be called with the given arguments.")
  public static class MethodsFromArgs extends Methods {

    @Override
    protected Obj getMatchingValueFromArgs(Context context, Obj left, Obj right) {
      return context.toObj(left, right);
    }

    @Override
    public boolean isMatchingMethod(Obj value, Context context, Callable method) {
      return PatternTester.test(context, method.getPattern(), value, method.getClosure());
    }
  }  
}
