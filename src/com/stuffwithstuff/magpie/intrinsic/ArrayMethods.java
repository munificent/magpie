package com.stuffwithstuff.magpie.intrinsic;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ArrayMethods {
  @Def("(is Array)[index is Int]")
  @Doc("Gets the array element at the given index.")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      int index = validateIndex(context, list, right.asInt());
      
      return list.get(index);
    }
  }

  @Def("(is Array) count")
  @Doc("Gets the number of elements in the array.")
  public static class Count implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> elements = left.asList();
      return context.toObj(elements.size());
    }
  }

  @Def("(is Array) toList")
  @Doc("Creates a new List containing the elements of the array. Does not\n" +
       "modify the original array.")
  public static class ToList implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      // Copy the array in case the list modifies it.
      List<Obj> elements = new ArrayList<Obj>(left.asList());
      return context.toList(elements);
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
