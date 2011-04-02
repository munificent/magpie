package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.Expr;

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
  }
  
  public String getName() { return mName; }
  public List<ClassObj> getParents() { return mParents; }
  public Map<String, Field> getFieldDefinitions() { return mFields; }
  
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

  public void declareField(String name, Expr type) {
    // Declare the field.
    mFields.put(name, new Field(null, type));
    
    // Add a getter and setter.
    /*
    mMembers.defineGetter(name, new FieldGetter(name));
    mMembers.defineSetter(name, new FieldSetter(name));
    */
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  private final String mName;
  
  private final List<ClassObj> mParents = new ArrayList<ClassObj>();
  
  private final Map<String, Field> mFields = new HashMap<String, Field>();
}
