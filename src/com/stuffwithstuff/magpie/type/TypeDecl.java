package com.stuffwithstuff.magpie.type;

/**
 * Base class for any kind of type declaration.
 */
public abstract class TypeDecl {
  // Accessor for the built-in "Dynamic" type.
  public static TypeDecl dynamic() { return sDynamic; }
  public static TypeDecl nothing() { return sNothing; }
  public static TypeDecl boolType() { return sBool; }
  // TODO(bob): This should go away. Should define specific function types based
  // on args and return.
  public static TypeDecl functionType() { return sFunction; }
  public static TypeDecl intType() { return sInt; }
  public static TypeDecl stringType() { return sString; }
  
  private static final TypeDecl sDynamic  = new NamedType("Dynamic");
  private static final TypeDecl sNothing  = new NamedType("Nothing");
  private static final TypeDecl sBool     = new NamedType("Bool");
  private static final TypeDecl sFunction = new NamedType("Function");
  private static final TypeDecl sInt      = new NamedType("Int");
  private static final TypeDecl sString   = new NamedType("String");
}
