package com.stuffwithstuff.magpie.parser;

public enum TokenType {
  // punctuation and grouping
  LEFT_PAREN,
  RIGHT_PAREN,
  LEFT_BRACKET,
  RIGHT_BRACKET,
  LEFT_BRACE,
  RIGHT_BRACE,
  COLON,
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
  BREAK,
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
  IN,
  INTERFACE,
  LET,
  MATCH,
  NOTHING,
  OR,
  RETURN,
  SHARED,
  THEN,
  THIS,
  TYPEOF,
  VAR,
  WHILE,
  
  // spacing
  LINE,
  EOF
}
