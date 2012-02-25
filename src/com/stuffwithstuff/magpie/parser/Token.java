package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.parser.TokenType;

/**
 * A single token of Magpie source code. Represents the smallest meaningful
 * chunk of sequential characters in a stream of Magpie code. Produced by the
 * lexer, and consumed by the parser.
 */
public final class Token {
  public Token(Position position, TokenType type, String text) {
    this(position, type, text, text);
  }
  
  public Token(Position position, TokenType type, String text, Object value) {
    mPosition = position;
    mType = type;
    mText = text;
    mValue = value;
  }

  public Position getPosition() { return mPosition; }
  
  public TokenType getType() { return mType; }
  public String    getText() { return mText; }
  
  public Object  getValue()  { return mValue; }
  public boolean getBool()   { return ((Boolean)mValue).booleanValue(); }
  public int     getInt()    { return ((Integer)mValue).intValue(); }
  public double  getDouble() { return ((Double)mValue).doubleValue(); }
  public String  getString() { return (String)mValue; }
  
  public String toString() {
    return mText;
  }
  
  private final Position  mPosition;
  private final TokenType mType;
  private final String    mText;
  private final Object    mValue;
}
