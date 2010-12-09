package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;

public class MemberSet extends HashMap<String, Member> {
  public void defineMethod(String name, Callable method) {
    put(name, new Member(MemberType.METHOD, method));
  }

  public void defineGetter(String name, Callable getter) {
    put(name, new Member(MemberType.GETTER, getter));
  }

  private static final long serialVersionUID = 1L;
}
