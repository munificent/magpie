package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.List;

import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ListBuiltIns {
  @Signature("(_ List)[index Int]")
  public static class Index implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      List<Obj> elements = arg.getTupleField(0).asList();
      int index = validateIndex(interpreter, elements,
          arg.getTupleField(1).asInt());
      
      return elements.get(index);
    }
  }

  @Signature("(_ List)[index Int] = (item)")
  public static class IndexAssign implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      List<Obj> elements = arg.getTupleField(0).asList();
      
      int index = validateIndex(interpreter, elements,
          arg.getTupleField(1).getTupleField(0).asInt());
      
      Obj value = arg.getTupleField(1).getTupleField(1);
      
      elements.set(index, value);
      return value;
    }
  }
  
  @Signature("(_ List) add(item)")
  public static class Add implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      List<Obj> elements = arg.getTupleField(0).asList();
      elements.add(arg.getTupleField(1));
      
      return arg.getTupleField(1);
    }
  }

  @Signature("(_ List) clear()")
  public static class Clear implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      List<Obj> elements = arg.getTupleField(0).asList();
      elements.clear();
      return interpreter.nothing();
    }
  }
  
  @Signature("(_ List) count")
  public static class Count implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj arg) {
      List<Obj> elements = arg.asList();
      return interpreter.createInt(elements.size());
    }
  }
  
  private static int validateIndex(Interpreter interpreter, List<Obj> list,
      int index) {
    // Negative indices count backwards from the end.
    if (index < 0) {
      index = list.size() + index;
    }
    
    // Check the bounds.
    if ((index < 0) || (index >= list.size())) {
      interpreter.error("OutOfBoundsError");
    }
    
    return index;
  }

  /*

  
  @Signature("insert(index Int, item ->)")
  public static class Insert implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int index = arg.getTupleField(0).asInt();
      Obj value = arg.getTupleField(1);
  
      List<Obj> elements = thisObj.asArray();
      elements.add(index, value);
      
      return interpreter.nothing();
    }
  }
  
  @Signature("removeAt(index Int)")
  public static class RemoveAt implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Obj> elements = thisObj.asArray();
      
      int index = arg.asInt();
      
      // Negative indices count backwards from the end.
      if (index < 0) {
        index = elements.size() + index;
      }
      
      return elements.remove(index);
    }
  }
  
  */
}
