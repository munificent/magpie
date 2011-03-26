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
  public List<Obj> getParents() { return mParents; }
  public Map<String, Field> getFieldDefinitions() { return mFields; }
  public MemberSet getMembers() { return mMembers; }
  
  /**
   * Gets whether or not this class is a subclass (or same class) as the given
   * parent.
   */
  public boolean isSubclassOf(ClassObj parent) {
    if (this == parent) return true;
    
    for (Obj myParent : mParents) {
      if (((ClassObj)myParent).isSubclassOf(parent)) return true;
    }
    
    return false;
  }
  
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
    
    // Try the parents. Look in reverse order so that the most-recently
    // inherited class takes priority over previous ones.
    for (int i = mParents.size() - 1; i >= 0; i--) {
      ClassObj parent = (ClassObj)mParents.get(i);
      member = ((ClassObj)parent).findMember(containingClass, name);
      if (member != null) return member;
    }
    
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
  
  private final String mName;
  
  // Note: This is List<Obj> instead of List<ClassObj> so that we can directly
  // expose it to Magpie code as a Magpie array.
  private final List<Obj> mParents = new ArrayList<Obj>();
  
  private final Map<String, Field> mFields = new HashMap<String, Field>();
  private final MemberSet mMembers;
}
