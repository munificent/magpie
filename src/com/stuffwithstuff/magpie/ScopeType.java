package com.stuffwithstuff.magpie;

/**
 * Defines the kind of scope in which a name can be bound. Local scope is used
 * for regular local variables and is defined using "var". Object scope is for
 * defining instance members in a class and uses "def". Class scope is for
 * defining shared members of a class and uses "shared".
 */
public enum ScopeType {
  LOCAL,
  OBJECT,
  CLASS
}
