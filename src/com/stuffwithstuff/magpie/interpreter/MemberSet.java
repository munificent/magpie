package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class MemberSet {
  public void defineMethod(String name, Callable method) {
    mMembers.put(name, new Member(MemberType.METHOD, method));
  }

  public void defineGetter(String name, Callable getter) {
    mMembers.put(name, new Member(MemberType.GETTER, getter));
  }

  public void defineSetter(String name, Callable setter) {
    // We munge the name by adding "_=" so that a setter member doesn't collide
    // with the corresponding getter of the same name.
    mMembers.put(name + "_=", new Member(MemberType.SETTER, setter));
  }

  public Member findMember(String name) {
    return mMembers.get(name);
  }

  public Set<Entry<String, Member>> entrySet() {
    return mMembers.entrySet();
  }

  private final Map<String, Member> mMembers = new HashMap<String, Member>();
}
