package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ObjectBuiltIns {
  @Getter("fields List(String, Dynamic)")
  public static class Fields implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Entry<String, Obj>> fields = new ArrayList<Entry<String, Obj>>(
          thisObj.getFields().entries());
      
      // Sort by name.
      Collections.sort(fields, new Comparator<Entry<String, Obj>>() {
        public int compare(Entry<String, Obj> left, Entry<String, Obj> right) {
          return left.getKey().compareTo(right.getKey());
        }
      });
      
      // Convert to a list of tuples.
      List<Obj> results = new ArrayList<Obj>();
      for (Entry<String, Obj> field : fields) {
        results.add(interpreter.createTuple(
            interpreter.createString(field.getKey()),
            field.getValue()));
      }
      
      return interpreter.createArray(results);
    }
  }
  
  @Signature("getField(name String)")
  public static class GetField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      Obj field = thisObj.getField(name);
      if (field == null) return interpreter.nothing();
      
      return field;
    }
  }

  @Signature("hasField?(name String)")
  public static class HasField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      Obj field = thisObj.getField(name);
      return interpreter.createBool(field != null);
    }
  }

  // TODO(bob): Make generic and allow returning something more specific?
  // TODO(bob): As neat as this is, it isn't type-safe in its current
  // incarnation. The problem is that the function passed to this was type-
  // checked with 'this' bound to a type based on where that function was
  // defined but now it's being evaluated with 'this' bound to a different type.
  // We'll either need to get rid of this (the easy solution), or come up with
  // a way to annotate what 'this' should be in a function type annotation.
  @Signature("receiving(block Nothing => Dynamic)")
  public static class Receiving implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj block = arg.asFn();
      
      // Ignore the function's bound receiver and use this object instead.
      return block.getCallable().invoke(interpreter, thisObj, arg);
    }
  }
}
