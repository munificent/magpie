package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.util.Expect;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
  public ClassObj(ClassObj metaclass, String name, ClassObj parent) {
    super(metaclass);
    mName = name;
    mParent = parent;
    mFieldInitializers = new HashMap<String, FnObj>();
    mGetters = new HashMap<String, Callable>();
  }

  public ClassObj(String name, ClassObj parent) {
    mName = name;
    mParent = parent;
    mFieldInitializers = new HashMap<String, FnObj>();
    mGetters = new HashMap<String, Callable>();
  }

  public Map<String, FnObj> getFieldInitializers() {
    return mFieldInitializers;
  }

  public Obj instantiate() {
    return new Obj(this);
  }

  public Obj instantiate(Object primitiveValue) {
    return new Obj(this, primitiveValue);
  }
  
  public String getName() { return mName; }
  
  public ClassObj getParent() { return mParent; }
  
  public void setParent(ClassObj parent) {
    mParent = parent;
  }
  
  public void addMethod(String name, Callable method) {
    mMethods.add(name, method);
  }
  
  public Map<String, Callable> getMethods() {
    return mMethods.getMethods();
  }
  
  public Callable findMethod(String name) {
    // Walk up the inheritance chain.
    ClassObj classObj = this;
    while (classObj != null) {
      Callable method = classObj.mMethods.find(name);
      if (method != null) return method;
      classObj = classObj.mParent;
    }
    
    // If we got here, it wasn't found.
    return null;
  }
  
  public Callable findGetter(String name) {
    // Walk up the inheritance chain.
    ClassObj classObj = this;
    while (classObj != null) {
      Callable getter = classObj.mGetters.get(name);
      if (getter != null) return getter;
      classObj = classObj.mParent;
    }
    
    // If we got here, it wasn't found.
    return null;
  }
  
  public void addConstructor(FnObj constructor) {
    Expect.notNull(constructor);
    
    mConstructor = constructor;
  }

  public Callable getConstructor() {
    return mConstructor;
  }
  
  public void defineField(String name, FnObj initializer) {
    mFieldInitializers.put(name, initializer);
  }
  
  public void defineGetter(String name, Callable body) {
    mGetters.put(name, body);
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private final String mName;
  private ClassObj mParent;
  private Callable mConstructor;
  private final Map<String, FnObj> mFieldInitializers;
  private final Map<String, Callable> mGetters;
  private final MethodSet mMethods = new MethodSet();
}
