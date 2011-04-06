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
  COLON,
  COMMA,
  DOT,
  EQUALS,
  
  // identifiers
  NAME,
  FIELD,      // a record field like "x:"

  // literals
  BOOL,
  DOUBLE,
  INT,
  NOTHING,
  STRING,
  
  // spacing
  LINE,
  EOF
}
