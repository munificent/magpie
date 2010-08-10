package com.stuffwithstuff.magpie.parser;

import java.util.*;

public abstract class Parser {
  public Parser(Lexer lexer) {
    mLexer = lexer;
    mRead = new LinkedList<Token>();
    mConsumed = new LinkedList<Token>();
  }

  public Token last(int offset) {
    return mConsumed.get(offset - 1);
  }

  public Token current() {
    return lookAhead(0);
  }
  
  public boolean lookAhead(TokenType... types) {
    // make sure all of the types match before we start consuming
    for (int i = 0; i < types.length; i++) {
      if (!lookAhead(i).getType().equals(types[i]))
        return false;
    }

    return true;
  }
  
  /**
   * Gets whether or not the next Token is of any of the given types.
   * @param  types The allowed types for the next Token.
   * @return       true if the Token is one of the types, false otherwise.
   */
  public boolean lookAheadAny(TokenType... types) {
    for (TokenType type : types) {
      if (lookAhead(type)) return true;
    }
    
    return false;
  }

  public boolean match(TokenType... types) {
    if (!lookAhead(types)) return false;

    // consume the matched tokens
    for (int i = 0; i < types.length; i++) {
      consume();
    }
    
    return true;
  }
  
  public void consume() {
    mConsumed.add(0, mRead.remove(0));
  }

  public Token consume(TokenType type) {
    if (match(type)) {
      return last(1);
    } else {
      String message = String.format("Expected token %s at %s, found %s.",
          type, current().getPosition(), current());
      throw new ParseException(message);
    }
  }

  public Token consumeAny(TokenType... types) {
    for (int i = 0; i < types.length; i++) {
      if (match(types[i])) return last(1);
    }
    
    StringBuilder builder = new StringBuilder();
    builder.append("Expected ");
    
    for (int i = 0; i < types.length; i++) {
      builder.append(types[i]);

      if (i < types.length - 2) {
        builder.append(", ");
      } else if (i < types.length - 2) {
        builder.append(" or ");
      }
    }
    
    builder.append(" at ")
           .append(current().getPosition())
           .append(" and found ")
           .append(current());
    throw new ParseException(builder.toString());
  }

  private Token lookAhead(int distance) {
    // read in as many as needed
    while (distance >= mRead.size()) {
      mRead.add(mLexer.readToken());
    }

    // get the queued token
    return mRead.get(distance);
  }

  private final Lexer mLexer;

  private final List<Token> mRead;
  private final List<Token> mConsumed;
}
