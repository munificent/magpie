package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.parser.Token;

/**
 * Converts a raw token sequence to a stream of just the meaningful ones.
 * Strips out comments and whitespace, and handles eliding newlines where
 * appropriate.
 * 
 * (Regarding the name: the Lexer generates "lexemes". This generates
 * "morphemes", the tokens that have semantic meaning.)
 */
public class Morpher implements TokenReader {

  public Morpher(TokenReader tokens) {
    mTokens = tokens;
    
    // Consume any leading newlines.
    mEatLines = true;
  }

  @Override
  public Token readToken() {
    while (true) {
      Token token = mTokens.readToken();
      
      switch (token.getType()) {
      case WHITESPACE:
      case BLOCK_COMMENT:
      case LINE_COMMENT:
        // Ignore non-semantic tokens.
        continue;
        
      // Ignore lines after tokens that can't end an expression.
      case BACKTICK:
      case COMMA:
      case FIELD:
      case LEFT_PAREN:
      case LEFT_BRACKET:
      case LEFT_BRACE:
      case ASTERISK:
      case SLASH:
      case PERCENT:
      case PLUS:
      case MINUS:
      case LT:
      case GT:
      case LTE:
      case GTE:
      case EQ:
      case EQEQ:
      case NOTEQ:
      case AND:
      case OR:
        mEatLines = true;
        break;

      case LINE_CONTINUATION:
        mEatLines = true;
        continue;
        
      case LINE:
        if (mEatLines) continue;
        
        // Collapse multiple lines into one.
        mEatLines = true;
        break;

      default:
        // A line after any other token is significant.
        mEatLines = false;
        break;
      }
      
      return token;
    }
  }

  private final TokenReader mTokens;
  private boolean mEatLines;
}
