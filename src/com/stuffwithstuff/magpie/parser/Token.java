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
  
  public boolean isKeyword(String name) {
    if (mType != TokenType.NAME) return false;
    return name.equals(getString());
  }
  
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
      case COLON: return ":";
      case COMMA: return ",";
      case DOT: return ".";
      case LINE: return "\n";

      case NAME: return getString();
      case FIELD: return getString() + ":";

      case BOOL: return Boolean.toString(getBool());
      case INT: return Integer.toString(getInt());
      case DOUBLE: return Double.toString(getDouble());
      case STRING: return "\"" + getString() + "\"";

      case EOF: return "(eof)";

      default: return "(unknown token?!)";
    }
  }
  
  private final Position  mPosition;
  private final TokenType mType;
  private final Object    mValue;
}
