package com.stuffwithstuff.magpie.intrinsic;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class ListMethods {
  @Def("(is List)[index is Int]")
  @Doc("Gets the element at the given index in the list.")
  public static class Index implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      int index = Indexable.validateIndex(context, list, right.asInt());
      
      return list.get(index);
    }
  }

  @Def("(is List)[index is Int] = (item)")
  @Doc("Replaces the element at the given index in the list. Returns the\n" +
       "assigned value.")
  public static class IndexAssign implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.getField(0).asList();
      
      int index = Indexable.validateIndex(context, list,
          left.getField(1).asInt());
      
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

  @Def("(== List) new()")
  @Doc("Creates a new empty list.")
  public static class New implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toList(new ArrayList<Obj>());
    }
  }

  @Def("(== List) new(fill: fill, size: size is Int)")
  @Doc("Creates a list filled with the given value.")
  public static class New_FillSize implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      Obj fill = right.getField("fill");
      int size = right.getField("size").asInt();
      
      List<Obj> elements = new ArrayList<Obj>();
      for (int i = 0; i < size; i++) {
        elements.add(fill);
      }
      
      return context.toList(elements);
    }
  }
  
  @Def("(is List) insert(item, at: index is Int)")
  @Doc("Inserts the given item at the given index in the list.")
  public static class Insert implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      Obj value = right.getField(0);
      
      int index = Indexable.validateIndex(context, list.size() + 1,
          right.getField("at").asInt());

      list.add(index, value);
      
      return value;
    }
  }

  @Def("(is List) toArray")
  @Doc("Creates a new array containing the same elements as the list.")
  public static class ToArray implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      List<Obj> list = left.asList();
      List<Obj> array = new ArrayList<Obj>();
      for (Obj element : list) {
        array.add(element);
      }
      
      return context.toArray(array);
    }
  }
}
