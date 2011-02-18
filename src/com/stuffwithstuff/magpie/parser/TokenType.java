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
  
  // identifiers
  NAME,
  FIELD,      // a record field like "x:"

  // literals
  BOOL,
  INT,
  DOUBLE,
  STRING,
  
  // spacing
  LINE,
  EOF
}
