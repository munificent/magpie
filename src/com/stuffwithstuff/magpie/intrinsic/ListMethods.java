package com.stuffwithstuff.magpie.intrinsic;

import java.util.List;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ListMethods {
  @Def("(is List)[index is Int]")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      List<Obj> elements = arg.getField(0).asList();
      int index = validateIndex(context, elements,
          arg.getField(1).asInt());
      
      return elements.get(index);
    }
  }

  @Def("(is List)[index is Int] = (item)")
  public static class IndexAssign implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      List<Obj> elements = arg.getField(0).asList();
      
      int index = validateIndex(context, elements,
          arg.getField(1).getField(0).asInt());
      
      Obj value = arg.getField(1).getField(1);
      
      elements.set(index, value);
      return value;
    }
  }
  
  @Def("(is List) add(item)")
  public static class Add implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      List<Obj> elements = arg.getField(0).asList();
      elements.add(arg.getField(1));
      
      return arg.getField(1);
    }
  }

  @Def("(is List) clear()")
  public static class Clear implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      List<Obj> elements = arg.getField(0).asList();
      elements.clear();
      return context.nothing();
    }
  }
  
  @Def("(is List) count")
  public static class Count implements Intrinsic {
    public Obj invoke(Context context, Obj arg) {
      List<Obj> elements = arg.asList();
      return context.toObj(elements.size());
    }
  }
  
  private static int validateIndex(Context context, List<Obj> list,
      int index) {
    // Negative indices count backwards from the end.
    if (index < 0) {
      index = list.size() + index;
    }
    
    // Check the bounds.
    if ((index < 0) || (index >= list.size())) {
      context.error(Name.OUT_OF_BOUNDS_ERROR, "Index " + index +
          " is out of bounds [0, " + list.size() + "].");
    }
    
    return index;
  }

  /*

  
  @Signature("insert(index Int, item ->)")
  public static class Insert implements BuiltInCallable {
    public Obj invoke(Context context, Obj thisObj, Obj arg) {
      int index = arg.getTupleField(0).asInt();
      Obj value = arg.getTupleField(1);
  
      List<Obj> elements = thisObj.asArray();
      elements.add(index, value);
      
      return interpreter.nothing();
    }
  }
  
  @Signature("removeAt(index Int)")
  public static class RemoveAt implements BuiltInCallable {
    public Obj invoke(Context context, Obj thisObj, Obj arg) {
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
