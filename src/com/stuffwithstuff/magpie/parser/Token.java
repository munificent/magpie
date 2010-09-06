package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.parser.TokenType;

/**
 * A single token of Magpie source code. Represents the smallest meaningful
 * chunk of sequential characters in a stream of Magpie code. Produced by the
 * lexer, and consumed by the parser.
 */
public final class Token {
  public Token(Position position, TokenType type) {
    mPosition = position;
    mType = type;
    mValue = null;
  }

  public Token(Position position, TokenType type, Object value) {
    mPosition = position;
    mType = type;
    mValue = value;
  }

  public Position getPosition() { return mPosition; }
  
  public TokenType getType() { return mType; }
  
  public boolean getBool()   { return ((Boolean)mValue).booleanValue(); }
  public int     getInt()    { return ((Integer)mValue).intValue(); }
  public double  getDouble() { return ((Double)mValue).doubleValue(); }
  public String  getString() { return (String)mValue; }
  
  public String toString() {
    switch (mType)
    {
      case LEFT_PAREN: return "(";
      case RIGHT_PAREN: return ")";
      case LEFT_BRACKET: return "[";
      case RIGHT_BRACKET: return "]";
      case LEFT_BRACE: return "{";
      case RIGHT_BRACE: return "}";
      case COMMA: return ",";
      case LINE: return "(line)";
      case DOT: return ".";
      case EQUALS: return "=";

      case NAME: return getString() + " (name)";
      case OPERATOR: return getString() + " (op)";

      case BOOL: return Boolean.toString(getBool());
      case INT: return Integer.toString(getInt());
      case DOUBLE: return Double.toString(getDouble());
      case STRING: return "\"" + getString() + "\"";

      case ARROW: return "->";
      case BREAK: return "break";
      case CASE: return "case";
      case CLASS: return "class";
      case DEF: return "def";
      case DO: return "do";
      case ELSE: return "else";
      case END: return "end";
      case EXTEND: return "extend";
      case FOR: return "for";
      case FN: return "fn";
      case IF: return "if";
      case LET: return "let";
      case MATCH: return "match";
      case NOTHING: return "nothing";
      case RETURN: return "return";
      case SHARED: return "shared";
      case THEN: return "then";
      case THIS: return "this";
      case TYPEOF: return "typeof";
      case VAR: return "var";
      case WHILE: return "while";

      case EOF: return "(eof)";

      default: return "(unknown token?!)";
    }
  }
  
  private final Position  mPosition;
  private final TokenType mType;
  private final Object    mValue;
}
