package com.stuffwithstuff.magpie.interpreter;

import com.stuffwithstuff.magpie.ast.Expr;

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
  
  /**
   * Evaluates the type of this member.
   * 
   * @return The member type.
   */
  public Obj evaluateType(Interpreter interpreter) {
    // TODO(bob): This is gross. The type of a property needs to be just the
    // value type, not a function that returns the value type. For built-in
    // properties like FieldGetter or anything defined in BuiltIns, they do that
    // automatically. For user-defined ones, we just have a definition
    // function whose return type is the value type, so we need to pull that
    // out.
    if (((mType == MemberType.GETTER) || (mType == MemberType.SETTER)) &&
        (mDefinition instanceof Function)) {
      Function function = (Function)mDefinition;
      Expr type = function.getFunction().getType().getReturnType();
      return interpreter.evaluate(type, interpreter.createTopLevelContext());
    }

    Obj type = mDefinition.getType(interpreter);
    
    return type;
  }
  
  private final MemberType mType;
  private final Callable   mDefinition;
}
