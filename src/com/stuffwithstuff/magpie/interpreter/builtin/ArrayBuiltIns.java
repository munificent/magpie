package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ArrayBuiltIns {
  @Shared
  @Signature("of(items -> Array newType(Object))")
  public static Obj of(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = new ArrayList<Obj>();
    if (arg == interpreter.nothing()) {
      // zero element array
    } else {
      Obj countObj = arg.getField(Identifiers.COUNT);
      // TODO(bob): Hackish. Checks for "count" to decide if arg is a tuple.
      // Should do something smarter.
      if (countObj != null) {
        int count = countObj.asInt();
        
        for (int i = 0; i < count; i++) {
          elements.add(arg.getTupleField(i));
        }
      } else {
       // a non-tuple arg means a one-element array
        elements.add(arg);
      }
    }
    
    return interpreter.createArray(elements);
  }

  @Getter("count(-> Int)")
  public static Obj count(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = thisObj.asArray();
    return interpreter.createInt(elements.size());
  }
  
  @Signature("call(index Int)")
  public static Obj call(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = thisObj.asArray();
    
    int index = arg.asInt();
    
    // Negative indices count backwards from the end.
    if (index < 0) {
      index = elements.size() + index;
    }
    
    return elements.get(index);
  }
  
  @Signature("assign(index Int, item)")
  public static Obj assign(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = thisObj.asArray();
    
    int index = arg.getTupleField(0).asInt();
    
    // Negative indices count backwards from the end.
    if (index < 0) {
      index = elements.size() + index;
    }
    
    elements.set(index, arg.getTupleField(1));
    return interpreter.nothing();
  }
  
  @Signature("add(item ->)")
  public static Obj add(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = thisObj.asArray();
    elements.add(arg);
    
    return interpreter.nothing();
  }
  
  @Signature("insert(index Int, item ->)")
  public static Obj insert(Interpreter interpreter, Obj thisObj, Obj arg) {
    int index = arg.getTupleField(0).asInt();
    Obj value = arg.getTupleField(1);

    List<Obj> elements = thisObj.asArray();
    elements.add(index, value);
    
    return interpreter.nothing();
  }
  
  @Signature("removeAt(index Int)")
  public static Obj removeAt(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = thisObj.asArray();
    return elements.remove(arg.asInt());
  }
  
  @Signature("clear(->)")
  public static Obj clear(Interpreter interpreter, Obj thisObj, Obj arg) {
    List<Obj> elements = thisObj.asArray();
    elements.clear();
    return interpreter.nothing();
  }
}
