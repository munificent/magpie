package com.stuffwithstuff.magpie.parser;

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
  EQUALS,
  
  // identifiers
  NAME,
  OPERATOR,

  // literals
  BOOL,
  INT,
  DOUBLE,
  STRING,
  
  // keywords
  AND,
  ARROW,
  CASE,
  CLASS,
  DEF,
  DO,
  ELSE,
  END,
  EXTEND,
  FN,
  FOR,
  IF,
  LET,
  MATCH,
  NOTHING,
  OR,
  RETURN,
  SHARED,
  THEN,
  THIS,
  VAR,
  WHILE,
  
  // spacing
  LINE,
  EOF
}
