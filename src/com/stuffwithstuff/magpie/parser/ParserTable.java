package com.stuffwithstuff.magpie.parser;

import java.util.HashMap;
import java.util.Map;

import com.stuffwithstuff.magpie.util.Expect;

/**
 * Registers and finds appropriate TokenParsers for a given Token.
 */
public class ParserTable<T> {
  public void define(String name, T parser) {
    Expect.notEmpty(name);
    Expect.notNull(parser);
    
    mNameTable.put(name, parser);
  }
  
  public boolean contains(String name) {
    return mNameTable.containsKey(name);
  }
  
  public void define(TokenType type, T parser) {
    Expect.notNull(parser);
    
    mTypeTable.put(type, parser);
  }
  
  public T get(String name) {
    return mNameTable.get(name);
  }
  
  public T get(Token token) {
    // A parser bound to a specific name takes priority over one bound to an
    // entire token type.
    if (token.getType() == TokenType.NAME) {
      T named = mNameTable.get(token.getString());
      if (named != null) return named;
    }
    
    return mTypeTable.get(token.getType());
  }
  
  private final Map<String, T> mNameTable = new HashMap<String, T>();
  private final Map<TokenType, T> mTypeTable = new HashMap<TokenType, T>();
}
