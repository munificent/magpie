package com.stuffwithstuff.magpie.parser;

import java.util.*;

import com.stuffwithstuff.magpie.util.Expect;

/**
 * Base class for a generic recursive descent parser with arbitrary lookahead.
 * Provides basic methods to look into and consume the token stream. Parsers for
 * specific grammars can then be built on top of those operations.
 */
public abstract class Parser {
  public Parser(Lexer lexer) {
    Expect.notNull(lexer);
    
    mLexer = lexer;
    mRead = new LinkedList<Token>();
    mConsumed = new LinkedList<Token>();
  }

  /**
   * Gets a previously consumed Token.
   * @param  offset  How far back in the token stream to read. One is the most
   *                 recently parsed token (i.e. read last(1) as "last one"),
   *                 two is the token before that, etc. The offset must be
   *                 positive. To get an unconsumed token, use current() or
   *                 lookAhead().
   * @return         The previously consumed token.
   */
  public Token last(int offset) {
    Expect.positive(offset);

    return mConsumed.get(offset - 1);
  }

  /**
   * Gets the current token in the token stream. This is the next token that
   * will be consumed.
   * 
   * @return The current token.
   */
  public Token current() {
    return lookAhead(0);
  }
  
  /**
   * Looks ahead at the token stream to see if the next tokens match the
   * expected types, in order. For example, if the next tokens are (123, true),
   * then a call to lookAhead(TokenType.INT, TokenType.BOOL) will return true.
   * Does not consume any tokens.
   * 
   * @param   types  The expected token types, in the order that they are
   *                 expected to appear.
   * @return         True if the next tokens are of the given types, false
   *                 otherwise.
   */
  public boolean lookAhead(TokenType... types) {
    // make sure all of the types match before we start consuming
    for (int i = 0; i < types.length; i++) {
      if (!lookAhead(i).getType().equals(types[i]))
        return false;
      
      // TODO(bob): Kinda gross. If we're looking for a NAME, we need to make
      // sure that it is *not* a claimed keyword.
      if ((types[i] == TokenType.NAME) && isKeyword(lookAhead(i).getString())) {
        return false;
      }
    }

    return true;
  }
  
  /**
   * Gets whether or not the next Token is of any of the given types. Does not
   * consume the token.
   * 
   * @param   types  The allowed types for the next Token.
   * @return         True if the Token is one of the types, false otherwise.
   */
  public boolean lookAheadAny(TokenType... types) {
    for (TokenType type : types) {
      if (lookAhead(type)) return true;
    }
    
    return false;
  }

  // TODO(bob): Hack temp until all keyword parsing is moved into Magpie.
  public Token consume(String keyword) {
    if (!match(keyword)) {
      String message = String.format("Expected keyword %s at %s, found %s.",
          keyword, current().getPosition(), current());
      throw new ParseException(message);
    }

    return last(1);
  }
  
  // TODO(bob): Hack temp until all keyword parsing is moved into Magpie.
  public boolean match(String... keywords) {
    // See if they match.
    if (!lookAhead(keywords)) return false;

    // Consume the matched tokens.
    for (int i = 0; i < keywords.length; i++) {
      consume();
    }
    
    return true;
  }

  // TODO(bob): Hack temp until all keyword parsing is moved into Magpie.
  public boolean match(TokenType type, String keyword) {
    if (!lookAhead(type, keyword)) return false;

    consume();
    consume();
    
    return true;
  }

  // TODO(bob): Hack temp until all keyword parsing is moved into Magpie.
  public boolean lookAhead(TokenType type, String keyword) {
    if (!lookAhead(type)) return false;
    if (lookAhead(1).getType() != TokenType.NAME) return false;
    if (!lookAhead(1).getString().equals(keyword)) return false;
    
    return true;
  }
  
  public boolean lookAhead(String... keywords) {
    // make sure all of the types match before we start consuming
    for (int i = 0; i < keywords.length; i++) {
      if (lookAhead(i).getType() != TokenType.NAME) return false;
      if (!lookAhead(i).getString().equals(keywords[i])) return false;
    }

    return true;
  }
  
  /**
   * Looks ahead at the token stream to see if the next tokens match the
   * expected types, in order. If so, they are all consumed.
   * 
   * @param   types  The expected token types, in the order that they are
   *                 expected to appear.
   * @return         True if the tokens matched and were consumed, false
   *                 otherwise.
   */
  public boolean match(TokenType... types) {
    // See if they match.
    if (!lookAhead(types)) return false;

    // Consume the matched tokens.
    for (int i = 0; i < types.length; i++) {
      consume();
    }
    
    return true;
  }

  /**
   * Looks ahead at the next token to see if it's any of the given allowed
   * types. If so, consumes it.
   * 
   * @param   types  The types the next token can be.
   * @return         True if it matched and was consumed, false otherwise.
   */
  public boolean matchAny(TokenType... types) {
    for (TokenType type : types) {
      if (match(type)) return true;
    }
    
    return false;
  }
  
  /**
   * Consumes the current token and advances to the next one.
   *
   * @return  The consumed token.
   */
  public Token consume() {
    // Make sure we've read the token.
    lookAhead(0);
    
    mConsumed.add(0, mRead.remove(0));
    return last(1);
  }

  /**
   * Consumes the current token and advances to the next one. The token is
   * required to be of the given type. If not, a ParseException will be thrown.
   * 
   * @param   type  The type that the current token must be.
   * @return        The consumed token.
   */
  public Token consume(TokenType type) {
    if (match(type)) {
      return last(1);
    } else {
      String message = String.format("Expected token %s at %s, found %s.",
          type, current().getPosition(), current());
      throw new ParseException(message);
    }
  }

  /**
   * Consumes the current token and advances to the next one. The token is
   * required to be one of the given types. If not, a ParseException will be
   * thrown.
   * 
   * @param   types  The types that are allowed for the current token.
   * @return         The consumed token.
   */
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
      } else if (i < types.length - 1) {
        builder.append(" or ");
      }
    }
    
    builder.append(" at ")
           .append(current().getPosition())
           .append(" and found ")
           .append(current());
    throw new ParseException(builder.toString());
  }

  protected abstract boolean isKeyword(String name);
  
  private Token lookAhead(int distance) {
    // Read in as many as needed.
    while (distance >= mRead.size()) {
      mRead.add(mLexer.readToken());
    }

    // Get the queued token.
    return mRead.get(distance);
  }

  private final Lexer mLexer;

  private final List<Token> mRead;
  private final List<Token> mConsumed;
}
