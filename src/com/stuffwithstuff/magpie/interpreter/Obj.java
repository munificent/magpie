package com.stuffwithstuff.magpie.interpreter;

public class Obj {  
  public Obj(ClassObj classObj, Object primitiveValue) {
    mClass = classObj;
    mPrimitiveValue = primitiveValue;
  }
  
  public Obj(ClassObj classObj) {
    this(classObj, null);
  }
  
  /**
   * This gets called in one of two contexts. It will either be instantiating
   * the ClassObj that represents ths class of classes, or it will be a
   * NativeMethodObj. For the former, it is its own class, for the latter it
   * will have no class at all.
   */
  public Obj() {
    // If we are a class, we're our own class.
    mClass = (this instanceof ClassObj) ? (ClassObj)this : null;
    mPrimitiveValue = null;
  }
  
  public ClassObj getClassObj() { return mClass; }
  public Object getPrimitiveValue() { return mPrimitiveValue; }
  
  /**
   * Adds the given member to the Obj.
   * @param name   The name of the member.
   * @param member The member's value.
   */
  public void add(String name, Obj member) {
    mScope.define(name, member);
  }
  
  /**
   * Assigns a new value to an existing member with the given name.
   * @param name
   * @param member
   * @return
   */
  public boolean assign(String name, Obj member) {
    return mScope.assign(name, member);
  }
  
  public Obj getMember(String name) {
    // See if it's specific to this instance.
    Obj member = mScope.get(name);
    if (member != null) return member;
    
    // Otherwise, see if the class defines it.
    member = mClass.getInstanceMember(name);
    return member;
  }
  
  public Scope getScope() { return mScope; }
  
  public Obj getTupleField(int index) {
    return mScope.get(Integer.toString(index));
  }
  
  public boolean asBool() {
    if (mPrimitiveValue instanceof Boolean) {
      return ((Boolean)mPrimitiveValue).booleanValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a boolean.", this));
  }
  
  public int asInt() {
    if (mPrimitiveValue instanceof Integer) {
      return ((Integer)mPrimitiveValue).intValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not an int.", this));
  }
  
  public String asString() {
    if (mPrimitiveValue instanceof String) {
      return (String)mPrimitiveValue;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a string.", this));
  }
  
  @Override
  public String toString() {
    // Use the object's name if it has one.
    Obj name = mScope.get("name");
    if (name != null) return name.getPrimitiveValue().toString();
    
    // Else try its value.
    if (mPrimitiveValue == null) return "()";
    return mPrimitiveValue.toString();
  }
  
  private final ClassObj mClass;
  private final Object mPrimitiveValue;
  private final Scope mScope = new Scope();
}
