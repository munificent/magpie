package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.parser.Token;

/**
 * Takes a stream of tokens and cleans up the annotations. Right now, that just
 * means taking a series of DOC_COMMENT tokens (one for each line) and
 * concatenating them into a single token.
 */
public class Annotator extends Parser implements TokenReader {

  public Annotator(TokenReader tokens) {
    super(tokens);
  }

  @Override
  public Token readToken() {
    while (true) {
      Token token = consume();
      
      if (token.getType() == TokenType.DOC_COMMENT) {
        // Collapse successive doc comments into a single token. Given:
        // 
        //  /// Line 1.
        //  /// Line 2.
        //
        // The Lexer will generate DOC_COMMENT, LINE, DOC_COMMENT, LINE. This
        // collapses that into a single multi-line comment.
        while (match(TokenType.LINE, TokenType.DOC_COMMENT)) {
          token = new Token(token.getPosition().union(last(1).getPosition()),
              TokenType.DOC_COMMENT, token.getText() + "\n" + last(1).getText(),
              token.getString() + "\n" + last(1).getString());
        }
      }
      
      return token;
    }
  }

  @Override
  protected boolean isReserved(String name) {
    return false;
  }
}
