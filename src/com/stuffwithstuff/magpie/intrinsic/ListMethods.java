package com.stuffwithstuff.magpie.intrinsic;

import java.util.List;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ListMethods {
  @Def("(is List)[index is Int]")
  @Doc("Gets the element at the given index in the list.")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      int index = validateIndex(context, list, right.asInt());
      
      return list.get(index);
    }
  }

  @Def("(is List)[index is Int] = (item)")
  @Doc("Replaces the element at the given index in the list. Returns the\n" +
       "assigned value.")
  public static class IndexAssign implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.getField(0).asList();
      
      int index = validateIndex(context, list, left.getField(1).asInt());
      
      Obj value = right;
      
      list.set(index, value);
      return value;
    }
  }
  
  @Def("(is List) add(item)")
  @Doc("Appends the item to the end of the list.")
  public static class Add implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> elements = left.asList();
      elements.add(right);
      
      return right;
    }
  }

  @Def("(is List) clear()")
  @Doc("Removes all items from the list.")
  public static class Clear implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> elements = left.asList();
      elements.clear();
      return context.nothing();
    }
  }
  
  @Def("(is List) count")
  @Doc("Returns the number of items in the list.")
  public static class Count implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> elements = left.asList();
      return context.toObj(elements.size());
    }
  }
  
  @Def("(is List) insert(item, at: index is Int)")
  @Doc("Inserts the given item at the given index in the list.")
  public static class Insert implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      Obj value = right.getField(0);
      
      int index = validateIndex(context, list.size() + 1,
          right.getField("at").asInt());

      list.add(index, value);
      
      return value;
    }
  }

  private static int validateIndex(Context context, List<Obj> list, int index) {
    return validateIndex(context, list.size(), index);
  }
  
  private static int validateIndex(Context context, int size, int index) {
    // Negative indices count backwards from the end.
    if (index < 0) {
      index = size + index;
    }
    
    // Check the bounds.
    if ((index < 0) || (index >= size)) {
      context.error(Name.OUT_OF_BOUNDS_ERROR, "Index " + index +
          " is out of bounds [0, " + size + "].");
    }
    
    return index;
  }
}
