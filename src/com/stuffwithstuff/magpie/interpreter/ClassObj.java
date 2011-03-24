package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.builtin.FieldGetter;
import com.stuffwithstuff.magpie.interpreter.builtin.FieldSetter;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
  /**
   * Creates a new class object.
   * 
   * @param metaclass  The class of this class (its metaclass).
   * @param name       The name of the class.
   */
  public ClassObj(ClassObj metaclass, String name) {
    super(metaclass);
    mName = name;
    mMembers = new MemberSet(this);
  }
  
  public String getName() { return mName; }
  public List<Obj> getMixins() { return mMixins; }
  public Map<String, Field> getFieldDefinitions() { return mFields; }
  public MemberSet getMembers() { return mMembers; }
  
  /**
   * Looks for a member with the given name on the given class.
   * 
   * @param classObj  The class where we're looking for the member.
   * @param name      The name of the member.
   * @return          The member if found, otherwise null.
   */
  public Member findMember(ClassObj containingClass, String name) {
    // Try this class.
    Member member = mMembers.findMember(name);
    if (member != null) return member;
    
    // Try the mixins.
    member = findMemberInMixins(this, name, new HashSet<ClassObj>());
    if (member != null) return member;
    
    // Not found.
    return null;
  }

  public void declareField(String name, Expr type) {
    // Declare the field.
    mFields.put(name, new Field(null, type));
    
    // Add a getter and setter.
    mMembers.defineGetter(name, new FieldGetter(name, type));
    mMembers.defineSetter(name, new FieldSetter(name, type));
  }
  
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
    
    // Try the mixins. Look in reverse order so that the most-recently mixed-in
    // class take priority over older mixins.
    for (int i = classObj.mMixins.size() - 1; i >= 0; i--) {
      ClassObj mixin = (ClassObj)classObj.mMixins.get(i);
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
  private final MemberSet mMembers;
}
