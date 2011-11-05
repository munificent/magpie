package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

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
      Map<String, FieldObj> fields, String doc) {
    super(classClass);
    mName = name;

    if (parents != null) {
      mParents = parents;
    } else {
      mParents = new ArrayList<ClassObj>();
    }
    
    mFields = fields;
    mDoc = doc;
  }
  
  public String getName() { return mName; }
  public List<ClassObj> getParents() { return mParents; }
  public Map<String, FieldObj> getFieldDefinitions() { return mFields; }
  public String getDoc() { return mDoc; }
  public Multimethod getInitMethod() { return mInit; }
  
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
   * 
   * @return The colliding class, if any, null otherwise.
   */
  public ClassObj checkForCollisions() {
    Set<ClassObj> reachedClasses = new HashSet<ClassObj>();
    reachedClasses.add(this);
    
    return checkForCollisions(reachedClasses, this);
  }
  
  /**
   * Stores the "init" multimethod for this class. ClassObj needs to keep track
   * of this so that when new() invokes it, it can find the one in the scope
   * where the class is defined so that any other specializations to it can be
   * found.
   */
  public void bindInitMultimethod(Multimethod init) {
    if (mInit != null) throw new IllegalStateException("Can only bind once.");
    mInit = init;
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private ClassObj checkForCollisions(Set<ClassObj> reachedClasses,
      ClassObj classObj) {
    for (ClassObj parent : classObj.getParents()) {
      if (reachedClasses.contains(parent)) return parent;
      
      reachedClasses.add(parent);
      ClassObj recursed = checkForCollisions(reachedClasses, parent);
      if (recursed != null) return recursed;
    }
    
    // If we got here, we didn't find a collision.
    return null;
  }
  
  private final String mName;
  
  private final List<ClassObj> mParents;
  private final Map<String, FieldObj> mFields;
  private final String mDoc;
  private Multimethod mInit;
}
