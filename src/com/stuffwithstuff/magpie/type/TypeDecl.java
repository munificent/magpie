package com.stuffwithstuff.magpie.type;

/**
 * Base class for any kind of type declaration.
 */
public abstract class TypeDecl {
  // Accessor for the built-in "Dynamic" type.
  public static TypeDecl dynamic() { return sDynamic; }
  public static TypeDecl nothing() { return sNothing; }
  
  private static final TypeDecl sDynamic = new NamedType("Dynamic");
  private static final TypeDecl sNothing = new NamedType("Nothing");
}
