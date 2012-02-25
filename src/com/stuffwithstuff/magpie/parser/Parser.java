package com.stuffwithstuff.magpie.parser;

import java.util.LinkedList;
import java.util.List;

import com.stuffwithstuff.magpie.util.Expect;

/**
 * Base class for a generic recursive descent parser with arbitrary lookahead.
 * Provides basic methods to look into and consume the token stream. Parsers for
 * specific grammars can then be built on top of those operations.
 */
public abstract class Parser {
  public Parser(TokenReader tokens) {
    Expect.notNull(tokens);
    
    mTokens = tokens;
    mRead = new LinkedList<Token>();
    mConsumed = new LinkedList<Token>();
  }
  
  /**
   * Creates a new PositionSpan that starts before the last consumed Token.
   */
  public PositionSpan span() {
    return new PositionSpan(this, last(1).getPosition());
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
   * expected types, in order. For example, if the next tokens are (123, "do"),
   * then a call to lookAhead(TokenType.INT, TokenType.DO) will return true.
   * Does not consume any tokens.
   * 
   * @param   tokens  The expected types.
   * @return          True if the next tokens match.
   */
  public boolean lookAhead(TokenType... tokens) {
    for (int i = 0; i < tokens.length; i++) {
      if (!lookAhead(i).is(tokens[i])) return false;
    }

    return true;
  }

  /**
   * Gets whether or not the next Token is of any of the given types. Does not
   * consume the token.
   * 
   * @param   tokens  The allowed types for the next Token.
   * @return          True if the Token is one of the types.
   */
  public boolean lookAheadAny(TokenType... tokens) {
    for (TokenType token : tokens) {
      if (lookAhead(token)) return true;
    }
    
    return false;
  }

  /**
   * Looks ahead at the token stream to see if the next tokens match the
   * expected types, in order. If so, they are all consumed.
   * 
   * @param   tokens  The expected types, in the order that they are
   *                  expected to appear.
   * @return          True if the tokens matched and were consumed.
   */
  public boolean match(TokenType... tokens) {
    // See if they match.
    if (!lookAhead(tokens)) return false;

    // Consume the matched tokens.
    for (int i = 0; i < tokens.length; i++) {
      consume();
    }
    
    return true;
  }

  /**
   * Looks ahead at the next token to see if it's any of the given allowed
   * types. If so, consumes it.
   * 
   * @param   tokens  The types the next token can be.
   * @return          True if it matched and was consumed.
   */
  public boolean matchAny(TokenType... tokens) {
    for (TokenType token : tokens) {
      if (match(token)) return true;
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
      Token current = current();
      String message = String.format("Expected token %s, found %s.",
          type, current);
      throw new ParseException(current.getPosition(), message);
    }
  }

  private Token lookAhead(int distance) {
    // Read in as many as needed.
    while (distance >= mRead.size()) {
      mRead.add(mTokens.readToken());
    }

    // Get the queued token.
    return mRead.get(distance);
  }

  private final TokenReader mTokens;
  private final List<Token> mRead;
  private final List<Token> mConsumed;
}
