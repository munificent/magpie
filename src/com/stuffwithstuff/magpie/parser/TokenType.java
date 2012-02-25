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
  
  // operators
  ASTERISK,
  SLASH,
  PERCENT,
  PLUS,
  MINUS,
  LT,
  GT,
  LTE,
  GTE,
  EQ,
  EQEQ,
  NOTEQ,
  
  // keywords
  AND,
  OR,
  
  // identifiers
  NAME,
  FIELD,      // a record field like "x:"

  // literals
  BOOL,
  DOUBLE,
  INT,
  NOTHING,
  STRING,
  
  // comments
  BLOCK_COMMENT,
  DOC_COMMENT,
  LINE_COMMENT,
  
  // spacing
  LINE,
  WHITESPACE,
  LINE_CONTINUATION,
  EOF
}
