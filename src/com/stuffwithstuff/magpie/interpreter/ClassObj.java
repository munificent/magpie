package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
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
   * @return          The member if found, otherwise null.
   */
  public static Member findMember(ClassObj classObj, Obj receiver,
      String name) {
    // If we aren't given the class because we're doing runtime lookup, just
    // infer it from the receiver.
    if (classObj == null) {
      classObj = receiver.getClassObj();
    }
    
    // Try this class.
    Member member = classObj.mMembers.findMember(name);
    if (member != null) return member;
    
    // Try the delegates.
    if (receiver != null) {
      // Looking up a member at runtime, so look at the actual delegate values.
      for (Entry<String, Field> field : classObj.mFields.entrySet()) {
        if (field.getValue().isDelegate()) {
          Obj delegate = receiver.getField(field.getKey());
          member = findMember(null, delegate, name);
          if (member != null) return member;
        }
      }
    } else {
      // Looking up a member during type-checking, so look at the declared
      // delegate field types.
      // TODO(bob): Implement me!
    }
    
    // Try the mixins.
    member = findMemberInMixins(classObj, name, new HashSet<ClassObj>());
    if (member != null) return member;
    
    // Not found.
    return null;
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
  public List<Obj> getMixins() { return mMixins; }
  public Map<String, Field> getFieldDefinitions() { return mFields; }
  public MemberSet getMembers() { return mMembers; }

  @Override
  public String toString() {
    return mName;
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
  private static Member findMemberInMixins(ClassObj classObj, String name,
      Set<ClassObj> tried) {
    
    // Try this class.
    Member member = classObj.mMembers.findMember(name);
    if (member != null) return member;

    tried.add(classObj);
    
    // Try the mixins.
    for (Obj mixin : classObj.mMixins) {
      member = findMemberInMixins((ClassObj)mixin, name, tried);
      if (member != null) return member;
    }
    
    // Not found.
    return null;
  }
  
  private final String mName;
  
  // Note: This is List<Obj> instead of List<ClassObj> so that we can directly
  // expose it to Magpie code as a Magpie array.
  private final List<Obj> mMixins = new ArrayList<Obj>();
  
  private final Map<String, Field> mFields = new HashMap<String, Field>();
  private final MemberSet mMembers = new MemberSet();
}
