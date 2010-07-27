package com.stuffwithstuff.magpie;

public enum TokenType {
  // punctuation and grouping
  LEFT_PAREN,
  RIGHT_PAREN,
  LEFT_BRACKET,
  RIGHT_BRACKET,
  LEFT_BRACE,
  RIGHT_BRACE,
  COMMA,
  DOT,
  
  // identifiers
  NAME,
  OPERATOR,

  // literals
  BOOL,
  INT,
  DOUBLE,
  STRING,
  
  // keywords
  CASE,
  DEF,
  DO,
  ELSE,
  END,
  IF,
  LET,
  MATCH,
  THEN,
  VAR,
  WHILE,
  
  // spacing
  LINE,
  EOF
}
