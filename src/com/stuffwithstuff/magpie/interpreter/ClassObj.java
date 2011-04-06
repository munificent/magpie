package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.Field;

/**
 * A runtime object representing a class.
 */
public class ClassObj extends Obj {
  /**
   * Creates a new class object.
   * 
   * @param classClass  The class of this class.
   * @param name        The name of the class.
   */
  public ClassObj(ClassObj classClass, String name, List<ClassObj> parents,
      Map<String, Field> fields, Scope closure) {
    super(classClass);
    mName = name;

    if (parents != null) {
      mParents = parents;
    } else {
      mParents = new ArrayList<ClassObj>();
    }
    
    mFields = fields;
    mClosure = closure;
  }
  
  public String getName() { return mName; }
  public List<ClassObj> getParents() { return mParents; }
  public Map<String, Field> getFieldDefinitions() { return mFields; }
  public Scope getClosure() { return mClosure; }
  
  /**
   * Gets whether or not this class is a subclass (or same class) as the given
   * parent.
   */
  public boolean isSubclassOf(ClassObj parent) {
    if (this == parent) return true;
    
    for (ClassObj myParent : mParents) {
      if (myParent.isSubclassOf(parent)) return true;
    }
    
    return false;
  }
  
  /**
   * Walks the inheritance tree to see if any class can be reached through more
   * than one path.
   */
  public boolean checkForCollisions() {
    Set<ClassObj> reachedClasses = new HashSet<ClassObj>();
    reachedClasses.add(this);
    
    return checkForCollisions(reachedClasses, this);
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private boolean checkForCollisions(Set<ClassObj> reachedClasses,
      ClassObj classObj) {
    for (ClassObj parent : classObj.getParents()) {
      if (reachedClasses.contains(parent)) return true;
      
      reachedClasses.add(parent);
      if (checkForCollisions(reachedClasses, parent)) return true;
    }
    
    // If we got here, we didn't find a collision.
    return false;
  }
  
  private final String mName;
  
  private final List<ClassObj> mParents;
  private final Map<String, Field> mFields;
  private final Scope mClosure;
}
