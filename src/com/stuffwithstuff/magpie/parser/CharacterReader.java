package com.stuffwithstuff.magpie.parser;

/**
 * Defines a source of characters that a Lexer can read from. Used to abstract
 * whether the Lexer is reading from a file, or from the REPL.
 */
public interface CharacterReader {
  char current();
  void advance();
  String lookAhead(int count);
}
