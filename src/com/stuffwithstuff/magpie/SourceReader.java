package com.stuffwithstuff.magpie;

/**
 * Defines a source of characters that a Lexer can read from. Used to abstract
 * whether the Lexer is reading from a file, or from the REPL.
 */
public interface SourceReader {
  String getDescription();
  
  char current();
  void advance();
}
