package com.stuffwithstuff.magpie.app;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Method;

public class ReplMethods {
  @Def("quit()")
  public static class Quit implements Method {
    @Override
    public Object call(Object left, Object right) {
      throw new QuitException();
    }
  }
}
