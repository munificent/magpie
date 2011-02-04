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
public class ParserTable<T> {
  public void define(String name, T parser) {
    mNameTable.put(name, parser);
  }
  
  public void define(TokenType type, T parser) {
    mTypeTable.put(type, parser);
  }
  
  public T get(Token token) {
    if (token.getType() == TokenType.NAME ||
        token.getType() == TokenType.OPERATOR) {
      // Note that we do this instead of checking if the value is null to
      // indicate key absence because we may have registered a key with no
      // parser to reserve the word.
      if (mNameTable.containsKey(token.toString())) {
        return mNameTable.get(token.toString());
      }
    }
    
    return mTypeTable.get(token.getType());
  }
  
  public boolean isReserved(String name) {
    return mNameTable.containsKey(name);
  }
  
  private final Map<String, T> mNameTable = new HashMap<String, T>();
  private final Map<TokenType, T> mTypeTable = new HashMap<TokenType, T>();
}
