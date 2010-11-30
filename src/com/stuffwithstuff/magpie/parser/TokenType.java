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
  CATCH,
  CLASS,
  DEF,
  DELEGATE,
  DO,
  ELSE,
  END,
  EXTEND,
  FN,
  FOR,
  GET,
  IF,
  INTERFACE,
  LET,
  MATCH,
  NOTHING,
  OR,
  RETURN,
  SET,
  SHARED,
  THEN,
  THIS,
  TYPEOF,
  UNSAFECAST,
  VAR,
  WHILE,
  WITH,
  
  // spacing
  LINE,
  EOF
}
