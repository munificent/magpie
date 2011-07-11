package com.stuffwithstuff.magpie.intrinsic;

import java.util.List;

import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Name;
import com.stuffwithstuff.magpie.interpreter.Obj;

/**
 * Utility class for indexable collections (Array, List, String).
 */
public class Indexable {
  public static int validateIndex(Context context, List<Obj> list, int index) {
    return validateIndex(context, list.size(), index);
  }
  
  public static int validateIndex(Context context, String string, int index) {
    return validateIndex(context, string.length(), index);
  }
  
  public static int validateIndex(Context context, int size, int index) {
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
