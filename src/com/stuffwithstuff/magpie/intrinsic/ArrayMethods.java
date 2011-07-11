package com.stuffwithstuff.magpie.intrinsic;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ArrayMethods {
  @Def("(is Array)[index is Int]")
  @Doc("Gets the array element at the given index.")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      int index = Indexable.validateIndex(context, list, right.asInt());
      
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

  @Def("(== Array) new()")
  @Doc("Creates a new empty array. Given that arrays are immutable, this\n" +
       "is not that useful (you can just do `[]`) but is provided for\n" +
       "consistency.")
  public static class New implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toArray(new ArrayList<Obj>());
    }
  }

  @Def("(== Array) new(fill: fill, size: size is Int)")
  @Doc("Creates an array filled with the given value.")
  public static class New_FillSize implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      Obj fill = right.getField("fill");
      int size = right.getField("size").asInt();
      
      List<Obj> elements = new ArrayList<Obj>();
      for (int i = 0; i < size; i++) {
        elements.add(fill);
      }
      
      return context.toArray(elements);
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
}
