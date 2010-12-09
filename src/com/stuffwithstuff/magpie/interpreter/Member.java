package com.stuffwithstuff.magpie.interpreter;

/**
 * Represents a declared or defined member in a class. This includes, methods,
 * getters, and setters.
 */
public class Member {
  public Member(MemberType type, Callable definition) {
    mType = type;
    mDefinition = definition;
  }
  
  /**
   * Gets the type of this member.
   * 
   * @return The member type.
   */
  public MemberType getType() { return mType; }
  
  /**
   * Gets the definition for this member.
   * 
   * @return The definition for the member.
   */
  public Callable getDefinition() { return mDefinition; }
  
  private final MemberType mType;
  private final Callable   mDefinition;
}
