package com.stuffwithstuff.magpie.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers and finds appropriate TokenParsers for a given Token. Note that
 * a null parser may be registered for a given name Token. That indicates that
 * the name is a reserved word (i.e can't be used as an identifier), but doesn't
 * have its own parsing behavior. This is for keywords like "then" or "else"
 * that are used in the middle of a mixfix expression.
 */
public class ParserTable {
  public void define(String name, TokenParser parser) {
    mNameTable.put(name, parser);
  }
  
  public void define(TokenType type, TokenParser parser) {
    mTypeTable.put(type, parser);
  }
  
  public TokenParser get(Token token) {
    if (token.getType() == TokenType.NAME ||
        token.getType() == TokenType.OPERATOR) {
      TokenParser named = mNameTable.get(token.toString());
      if (named != null) return named;
    }
    
    return mTypeTable.get(token.getType());
  }
  
  public boolean isReserved(String name) {
    return mNameTable.containsKey(name);
  }
  
  private final Map<String, TokenParser> mNameTable =
      new HashMap<String, TokenParser>();
  private final Map<TokenType, TokenParser> mTypeTable =
      new HashMap<TokenType, TokenParser>();
}
