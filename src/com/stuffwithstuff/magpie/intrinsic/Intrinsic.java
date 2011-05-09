package com.stuffwithstuff.magpie.intrinsic;

import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public interface Intrinsic {
  Obj invoke(Context context, Obj left, Obj right);
}
