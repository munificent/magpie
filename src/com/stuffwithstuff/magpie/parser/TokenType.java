package com.stuffwithstuff.magpie.parser;

public enum TokenType {
  // punctuation and grouping
  LEFT_PAREN,
  RIGHT_PAREN,
  LEFT_BRACKET,
  RIGHT_BRACKET,
  LEFT_BRACE,
  RIGHT_BRACE,
  BACKTICK,
  COMMA,
  DOT,
  EQUALS,
  
  // identifiers
  NAME,
  FIELD,      // a record field like "x:"
  TYPE_PARAM, // a type parameter in a pattern like 'T

  // literals
  BOOL,
  INT,
  DOUBLE,
  STRING,
  
  // spacing
  LINE,
  EOF
}
