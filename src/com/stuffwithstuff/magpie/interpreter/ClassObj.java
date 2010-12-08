package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.util.Expect;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {  
  /**
   * Looks for a method with the given name on the given class. If the class is
   * not given, it will be inferred from the class of the receiver. The receiver
   * is optional. If not given, this will do "static" member lookup based on the
   * declared types of any delegate fields. This is used during type-checking.
   * If the receiver is given, it will look at the actual value's stored in the
   * receiver's delegate fields and delegate to this. This is used during
   * runtime evaluation.
   * 
   * @param classObj  The class where we're looking for the method. Should be
   *                  null if we want to infer it from the receiver.
   * @param receiver  The receiving object that the method is being invoked on.
   *                  Can be null if we're just looking up a method for type-
   *                  checking.
   * @param name      The name of the method.
   * @return          The method if found, otherwise null.
   */
  public static Callable findMethod(ClassObj classObj, Obj receiver,
      String name) {
    return findMember(classObj, receiver, name, new MethodLookup());
  }
  
  /**
   * Looks for a getter with the given name on the given class. If the class is
   * not given, it will be inferred from the class of the receiver. The receiver
   * is optional. If not given, this will do "static" member lookup based on the
   * declared types of any delegate fields. This is used during type-checking.
   * If the receiver is given, it will look at the actual value's stored in the
   * receiver's delegate fields and delegate to this. This is used during
   * runtime evaluation.
   * 
   * @param classObj  The class where we're looking for the getter. Should be
   *                  null if we want to infer it from the receiver.
   * @param receiver  The receiving object that the getter is being invoked on.
   *                  Can be null if we're just looking up a getter for type-
   *                  checking.
   * @param name      The name of the getter.
   * @return          The getter if found, otherwise null.
   */
  public static Callable findGetter(ClassObj classObj, Obj receiver,
      String name) {
    return findMember(classObj, receiver, name, new GetterLookup());
  }
  
  /**
   * Looks for a setter with the given name on the given class. If the class is
   * not given, it will be inferred from the class of the receiver. The receiver
   * is optional. If not given, this will do "static" member lookup based on the
   * declared types of any delegate fields. This is used during type-checking.
   * If the receiver is given, it will look at the actual value's stored in the
   * receiver's delegate fields and delegate to this. This is used during
   * runtime evaluation.
   * 
   * @param classObj  The class where we're looking for the setter. Should be
   *                  null if we want to infer it from the receiver.
   * @param receiver  The receiving object that the setter is being invoked on.
   *                  Can be null if we're just looking up a setter for type-
   *                  checking.
   * @param name      The name of the setter.
   * @return          The setter if found, otherwise null.
   */
  public static Callable findSetter(ClassObj classObj, Obj receiver,
      String name) {
    return findMember(classObj, receiver, name, new SetterLookup());
  }

  /**
   * Creates a new class object.
   * 
   * @param metaclass  The class of this class (its metaclass).
   * @param name       The name of the class.
   */
  // TODO(bob): Get rid of name?
  public ClassObj(ClassObj metaclass, String name) {
    super(metaclass);
    mName = name;
  }
  
  public String getName() { return mName; }
  public List<ClassObj> getMixins() { return mMixins; }
  public Map<String, Field> getFieldDefinitions() { return mFields; }
  public Map<String, Callable> getMethods() { return mMethods; }
  public Map<String, Callable> getGetters() { return mGetters; }
  public Map<String, Callable> getSetters() { return mSetters; }

  @Override
  public String toString() {
    return mName;
  }

  // TODO(bob): This should go away.
  public void addConstructor(Callable constructor) {
    Expect.notNull(constructor);

    mConstructor = constructor;
  }
  
  // TODO(bob): This should go away.
  public Callable getConstructor() {
    return mConstructor;
  }

  /**
   * Looks for a member with the given name on the given class. If the class is
   * not given, it will be inferred from the class of the receiver. The receiver
   * is optional. If not given, this will do "static" member lookup based on the
   * declared types of any delegate fields. This is used during type-checking.
   * If the receiver is given, it will look at the actual value's stored in the
   * receiver's delegate fields and delegate to this. This is used during
   * runtime evaluation.
   * 
   * @param classObj  The class where we're looking for the member. Should be
   *                  null if we want to infer it from the receiver.
   * @param receiver  The receiving object that the member is being invoked on.
   *                  Can be null if we're just looking up a member for type-
   *                  checking.
   * @param name      The name of the member.
   * @param lookup    The function to look up the member.
   * @return          The member if found, otherwise null.
   */
  private static Callable findMember(ClassObj classObj, Obj receiver,
      String name, MemberLookup lookup) {
    // If we aren't given the class because we're doing runtime lookup, just
    // infer it from the receiver.
    if (classObj == null) {
      classObj = receiver.getClassObj();
    }
    
    // Try this class.
    Callable member = lookup.find(classObj, name);
    if (member != null) return member;
    
    // Try the delegates.
    if (receiver != null) {
      // Looking up a member at runtime, so look at the actual delegate values.
      for (Entry<String, Field> field : classObj.mFields.entrySet()) {
        if (field.getValue().isDelegate()) {
          Obj delegate = receiver.getField(field.getKey());
          member = findMember(null, delegate, name, lookup);
          if (member != null) return member;
        }
      }
    } else {
      // Looking up a member during type-checking, so look at the declared
      // delegate field types.
      // TODO(bob): Implement me!
    }
    
    // Try the mixins.
    member = findMemberInMixins(classObj, name, lookup,
        new HashSet<ClassObj>());
    if (member != null) return member;
    
    // Not found.
    return null;
  }

  /**
   * Looks for a member in the mixin graph of a given class. Ensures that
   * classes that have already been searched are not looked at again so that
   * cyclic mixin graphs are safe.
   * 
   * @param classObj  The class being searched.
   * @param name      The name of the member being sought.
   * @param lookup    The function to look up the member.
   * @param tried     The set of classes that have already been searched.
   * @return          The member if found, otherwise null.
   */
  private static Callable findMemberInMixins(ClassObj classObj, String name,
      MemberLookup lookup, Set<ClassObj> tried) {
    
    // Try this class.
    Callable member = lookup.find(classObj, name);
    if (member != null) return member;

    tried.add(classObj);
    
    // Try the mixins.
    for (ClassObj mixin : classObj.mMixins) {
      member = findMemberInMixins(mixin, name, lookup, tried);
      if (member != null) return member;
    }
    
    // Not found.
    return null;
  }
  
  // TODO(bob): This implies that different member types exist in their own
  // namespace. That's probably not what we want long term. Unify these three
  // hashes to a single Map<String, Member>()?
  /**
   * Defines a function that will look for a named member of some type on a
   * class. Used to be able to search methods, getters, and setters generically.
   */
  private interface MemberLookup {
    public Callable find(ClassObj classObj, String name);
  }
  
  /**
   * MemberLookup function that searches a class's methods.
   */
  private static class MethodLookup implements MemberLookup {
    public Callable find(ClassObj classObj, String name) {
      return classObj.mMethods.get(name);
    }
  }
  
  /**
   * MemberLookup function that searches a class's getters.
   */
  private static class GetterLookup implements MemberLookup {
    public Callable find(ClassObj classObj, String name) {
      return classObj.mGetters.get(name);
    }
  }
  
  /**
   * MemberLookup function that searches a class's setters.
   */
  private static class SetterLookup implements MemberLookup {
    public Callable find(ClassObj classObj, String name) {
      return classObj.mSetters.get(name);
    }
  }
  
  private final String mName;
  // TODO(bob): Get rid of this.
  private Callable mConstructor;
  private final List<ClassObj> mMixins = new ArrayList<ClassObj>();
  private final Map<String, Field> mFields = new HashMap<String, Field>();
  private final Map<String, Callable> mGetters =
      new HashMap<String, Callable>();
  private final Map<String, Callable> mSetters =
      new HashMap<String, Callable>();
  private final Map<String, Callable> mMethods =
      new HashMap<String, Callable>();
}
