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
  
  public Object  getValue()  { return mValue; }
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
      case BACKTICK: return "`";
      case COMMA: return ",";
      case DOT: return ".";
      case EQUALS: return "=";
      case LINE: return "(line)";

      case NAME: return getString() + " (name)";
      case FIELD: return getString() + ":";
      case OPERATOR: return getString() + " (op)";

      case BOOL: return Boolean.toString(getBool());
      case INT: return Integer.toString(getInt());
      case DOUBLE: return Double.toString(getDouble());
      case STRING: return "\"" + getString() + "\"";

      case ARROW: return "->";
      case CASE: return "case";
      case CATCH: return "catch";
      case CLASS: return "class";
      case DEF: return "def";
      case DELEGATE: return "delegate";
      case ELSE: return "else";
      case END: return "end";
      case EXTEND: return "extend";
      case FOR: return "for";
      case FN: return "fn";
      case GET: return "get";
      case IF: return "if";
      case INTERFACE: return "interface";
      case LET: return "let";
      case MATCH: return "match";
      case NOTHING: return "nothing";
      case SET: return "set";
      case SHARED: return "shared";
      case THEN: return "then";
      case THIS: return "this";
      case WHILE: return "while";
      case WITH: return "with";

      case EOF: return "(eof)";

      default: return "(unknown token?!)";
    }
  }
  
  private final Position  mPosition;
  private final TokenType mType;
  private final Object    mValue;
}
