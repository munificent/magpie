package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.util.Expect;
import com.stuffwithstuff.magpie.util.Fn;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
  // TODO(bob): This is kind of gross. There are two code paths for looking up
  // a method: the static one and the dynamic one. The dynamic path, implemented
  // here, finds a delegated member by looking at the actual objects stored in
  // the delegate fields of some object. The static path, not fully implemented
  // yet, just looks at the type annotations. The reason is this:
  //
  // interface Foo
  //     def bar()
  // end
  //
  // FooImpl
  //     def bar() print("FooImpl")
  // end
  //
  // class Bang
  //     delegate var foo = FooImpl new()
  // end
  //
  // Bang new() bar()
  //
  // When we look up "bar" at runtime on the last line, it does this:
  // - Look in Bang for it (not found)
  // - Look at each field that's declared to be a delegate ("foo")
  // - Look at the *value* of that field in the actual receiving object (finding
  //   an instance of FooImpl)
  // - Look up the member there (finding bar() print("FooImpl"))
  //
  // Note that we may find a different delegate class at runtime as long as its
  // a subtype of the declared type. So we actually need to look at instance
  // state to do the delegation.
  //
  // At type-check time, though, there is no instance state. Instead, all we'll
  // be doing is looking at the declared type of the delegate fields directly
  // on the class and type-check against that. Hence: two code paths.
  public static Callable findMethod(Obj receiver, final String name) {
    return findMember(receiver, new Fn<ClassObj, Callable>() {
      public Callable apply(ClassObj classObj) {
        return classObj.mMethods.get(name);
      }
    });
  }
  
  public static Callable findGetter(Obj receiver, final String name) {
    return findMember(receiver, new Fn<ClassObj, Callable>() {
      public Callable apply(ClassObj classObj) {
        return classObj.mGetters.get(name);
      }
    });
  }
  
  public static Callable findSetter(Obj receiver, final String name) {
    return findMember(receiver, new Fn<ClassObj, Callable>() {
      public Callable apply(ClassObj classObj) {
        return classObj.mSetters.get(name);
      }
    });
  }
  
  public ClassObj(ClassObj metaclass, String name, ClassObj parent) {
    super(metaclass);
    mName = name;
    mParent = parent;
    mFields = new HashMap<String, Field>();
    mGetters = new HashMap<String, Callable>();
    mSetters = new HashMap<String, Callable>();
  }
  
  public ClassObj(String name, ClassObj parent) {
    mName = name;
    mParent = parent;
    mFields = new HashMap<String, Field>();
    mGetters = new HashMap<String, Callable>();
    mSetters = new HashMap<String, Callable>();
  }
  
  public Map<String, Field> getFieldDefinitions() {
    return mFields;
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
    mMethods.put(name, method);
  }
  
  public Map<String, Callable> getMethods() {
    return mMethods;
  }
  
  public Map<String, Callable> getGetters() {
    return mGetters;
  }
  
  public void addConstructor(Callable constructor) {
    Expect.notNull(constructor);

    mConstructor = constructor;
  }
  
  public Callable getConstructor() {
    return mConstructor;
  }
  
  // TODO(bob): These non-static find_ methods just do the old parent lookup.
  // Eventually, these need to do the delegate lookup that the static
  // find___() methods do.
  
  public Callable findMethod(String name) {
    ClassObj classObj = this;
    while (classObj != null) {
      Callable member = classObj.mMethods.get(name);
      if (member != null) return member;
      classObj = classObj.mParent;
    }
    return null;
  }
  
  public Callable findGetter(String name) {
    ClassObj classObj = this;
    while (classObj != null) {
      Callable member = classObj.mGetters.get(name);
      if (member != null) return member;
      classObj = classObj.mParent;
    }
    return null;
  }
  
  public Callable findSetter(String name) {
    ClassObj classObj = this;
    while (classObj != null) {
      Callable member = classObj.mSetters.get(name);
      if (member != null) return member;
      classObj = classObj.mParent;
    }
    return null;
  }

  public void declareField(String name, boolean isDelegate, Function type) {
    mFields.put(name, new Field(false, isDelegate, type));
  }
  
  public void defineField(String name, boolean isDelegate, Function initializer) {
    mFields.put(name, new Field(true, isDelegate, initializer));
  }
  
  public void defineGetter(String name, Callable body) {
    mGetters.put(name, body);
  }
  
  public void defineSetter(String name, Callable body) {
    mSetters.put(name, body);
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private static <T> T findMember(Obj receiver, Fn<ClassObj, T> lookup) {
    ClassObj classObj = receiver.getClassObj();
    
    // Try this class.
    T member = lookup.apply(classObj);
    if (member != null) return member;
    
    // Try the delegates.
    for (Entry<String, Field> field : classObj.mFields.entrySet()) {
      if (field.getValue().isDelegate()) {
        Obj delegate = receiver.getField(field.getKey());
        member = findMember(delegate, lookup);
        if (member != null) return member;
      }
    }
    
    // Old parent lookup:
    // TODO(bob): Get rid of this once delegation is used for everything.
    classObj = receiver.getClassObj();
    while (classObj != null) {
      member = lookup.apply(classObj);
      if (member != null) return member;
      classObj = classObj.mParent;
    }
    
    // New delegate hotness:
    // Not found.
    return null;
  }
  
  private final String mName;
  private Callable mConstructor;
  private ClassObj mParent;
  private final Map<String, Field> mFields;
  private final Map<String, Callable> mGetters;
  private final Map<String, Callable> mSetters;
  private final Map<String, Callable> mMethods = new HashMap<String, Callable>();
}
