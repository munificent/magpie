package com.stuffwithstuff.magpie.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class MemberSet {
  public MemberSet(ClassObj owningClass) {
    mClass = owningClass;
  }
  
  public void defineMethod(String name, Callable method) {
    name = mungePrivateName(name);
    mMembers.put(name, new Member(MemberType.METHOD, method));
  }

  public void defineGetter(String name, Callable getter) {
    name = mungePrivateName(name);
    mMembers.put(name, new Member(MemberType.GETTER, getter));
  }

  public void defineSetter(String name, Callable setter) {
    name = mungePrivateName(name);
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
  
  private String mungePrivateName(String name) {
    if (!Name.isPrivate(name)) return name;
    return Name.makeClassPrivate(mClass.getName(), name);
  }

  private final ClassObj mClass;
  private final Map<String, Member> mMembers = new HashMap<String, Member>();
}
