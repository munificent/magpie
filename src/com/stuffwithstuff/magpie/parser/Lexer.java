package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;

public class Lexer {

  public Lexer(String sourceFile, CharacterReader text) {
    mSourceFile = sourceFile;
    mText = text;
    mState = LexState.DEFAULT;
    mLine = 1;
    mCol = 1;
    
    // Ignore starting lines.
    mEatLines = true;
  }

  public Token readToken() {
    Token token = null;
    while (token == null) {
      token = readRawToken();
      if (token != null) token = normalizeLines(token);
    }
    
    return token;
  }

  private Token normalizeLines(Token token) {
    switch (token.getType()) {
    // Ignore lines after tokens that can't end an expression.
    case BACKTICK:
    case COMMA:
    case DOT:
    case OPERATOR:
    case FIELD:
    case ARROW:
    case LEFT_PAREN:
    case LEFT_BRACKET:
    case LEFT_BRACE:
      mEatLines = true;
      return token;

    case LINE:
      if (mEatLines) return null;
      
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
  
  private Token readRawToken() {
    char c = mText.current();
    
    switch (mState) {
    case DEFAULT:
      if (match("(")) return characterToken(TokenType.LEFT_PAREN);
      if (match(")")) return characterToken(TokenType.RIGHT_PAREN);
      if (match("[")) return characterToken(TokenType.LEFT_BRACKET);
      if (match("]")) return characterToken(TokenType.RIGHT_BRACKET);
      if (match("{")) return characterToken(TokenType.LEFT_BRACE);
      if (match("}")) return characterToken(TokenType.RIGHT_BRACE);
      if (match("`")) return characterToken(TokenType.BACKTICK);
      if (match(",")) return characterToken(TokenType.COMMA);
      if (match(".")) return characterToken(TokenType.DOT);

      // Match line ending characters.
      if (match(";"))  return characterToken(TokenType.LINE);
      if (match("\n")) return characterToken(TokenType.LINE);
      if (match("\r")) return characterToken(TokenType.LINE);
      
      // Comments.
      if (match("//")) return startToken(LexState.IN_LINE_COMMENT);
      if (match("/*")) return startToken(LexState.IN_BLOCK_COMMENT);
      
      if (lookAhead("\"")) return startToken(LexState.IN_STRING);
      if (lookAhead("-")) return startToken(LexState.IN_MINUS);
      
      if (isAlpha(c)) return startToken(LexState.IN_NAME);
      if (isOperator(c)) return startToken(LexState.IN_OPERATOR);
      if (isDigit(c)) return startToken(LexState.IN_NUMBER);
      
      if (lookAhead("'")) return startToken(LexState.IN_TYPE_PARAM);
      
      // Line continuation.
      if (lookAhead("\\")) return startToken(LexState.IN_LINE_CONTINUATION);

      // Ignore whitespace.
      if (match(" ")) return null;
      if (match("\t")) return null;
      
      // TODO(bob): Hack temp. Unexpected character
      return new Token(lastCharacterPosition(), TokenType.EOF);

    case IN_NAME:
      if (lookAhead("//")) {
        return createStringToken(TokenType.NAME);
      }
      if (lookAhead("/*")) {
        return createStringToken(TokenType.NAME);
      }
      if (c == ':') {
        String text = mRead;

        advance();
        mState = LexState.DEFAULT;
        
        Position position = currentPosition();        
        return new Token(position, TokenType.FIELD, text);
      }
      if (isAlpha(c) || isDigit(c) || isOperator(c)) {
        return advance();
      }
      return createStringToken(TokenType.NAME);

    case IN_OPERATOR:
      if (lookAhead("//")) {
        return createStringToken(TokenType.OPERATOR);
      }
      if (lookAhead("/*")) {
        return createStringToken(TokenType.OPERATOR);
      }
      if (isAlpha(c) || isDigit(c)) {
        return changeToken(LexState.IN_NAME);
      }
      if (isOperator(c)) {
        return advance();
      }
      return createStringToken(TokenType.OPERATOR);

    case IN_NUMBER:
      if (isDigit(c)) {
        return advance();
      }
      if (c == '.') {
        return changeToken(LexState.IN_DECIMAL);
      }
      return createIntToken(TokenType.INT);

    case IN_DECIMAL:
      if (isDigit(c)) {
        return changeToken(LexState.IN_FRACTION);
      }

      // Rollback to reprocess the dot.
      // TODO(bob): Rollback CharacterReader.
      mCol--;
      return createIntToken(TokenType.INT);
      
    case IN_FRACTION:
      if (isDigit(c)) {
        return advance();
      }
      return createDoubleToken(TokenType.DOUBLE);
      
    case IN_MINUS:
      if (lookAhead("//")) {
        return createStringToken(TokenType.OPERATOR);
      }
      if (lookAhead("/*")) {
        return createStringToken(TokenType.OPERATOR);
      }
      if (isDigit(c)) {
        return changeToken(LexState.IN_NUMBER);
      }
      if (isOperator(c) || isAlpha(c)) {
        return changeToken(LexState.IN_OPERATOR);
      }
      return createStringToken(TokenType.OPERATOR);

    case IN_STRING:
      if (match("\"")) {
        // Get the contained string without the quotes.
        String text = mRead.substring(1, mRead.length() - 1);
        mState = LexState.DEFAULT;
        return new Token(currentPosition(), TokenType.STRING, text);
      } else if (lookAhead("\\")) {
        mState = LexState.IN_STRING_ESCAPE;
        advance(false);
        return null;
      }
      // Consume other characters.
      return advance();
      
    case IN_STRING_ESCAPE:
      if (lookAhead("\\")) {
        mRead += "\\";
        mState = LexState.IN_STRING;
        advance(false);
        return null;
      } else if (lookAhead("\"")) {
        mRead += "\"";
        mState = LexState.IN_STRING;
        advance(false);
        return null;
      } else if (lookAhead("n")) {
        mRead += "\n";
        mState = LexState.IN_STRING;
        advance(false);
        return null;
      }
      throw new ParseException("Unknown string escape character " + c + ".");

    case IN_TYPE_PARAM:
      if (lookAhead("//")) {
        return createStringToken(TokenType.TYPE_PARAM);
      }
      if (lookAhead("/*")) {
        return createStringToken(TokenType.TYPE_PARAM);
      }
      if (isAlpha(c) || isDigit(c) || isOperator(c)) {
        return advance();
      }
      
      // Get the symbol name without the leading '.
      String text = mRead.substring(1);
      mState = LexState.DEFAULT;
      return new Token(currentPosition(), TokenType.TYPE_PARAM, text);
      
    case IN_LINE_COMMENT:
      if (match("\n") || match("\r")) {
        mState = LexState.DEFAULT;
        return new Token(lastCharacterPosition(), TokenType.LINE, last(1));
      }
      // Ignore everything else.
      return advance();
      
    case IN_BLOCK_COMMENT:
      if (match("*/")) {
        mState = LexState.DEFAULT;
        return null;
      }
      // Ignore everything else.
      return advance();
      
    case IN_LINE_CONTINUATION:
      // Ignore whitespace.
      if (match(" ")) return null;
      if (match("\t")) return null;

      // Eat the newline.
      if (lookAhead("\n") || lookAhead("\r")) {
        return changeToken(LexState.DEFAULT);
      }
      throw new ParseException("Unexpected character " + c + " after line continuation.");
      
    default:
      throw new ParseException("Unexpected lex state.");
    }
  }
  
  private boolean lookAhead(String text) {
    return mText.lookAhead(text.length()).equals(text);
  }

  private boolean match(String text) {
    boolean matched = lookAhead(text);
    
    if (matched) advance(text.length());
    return matched;
  }
  
  private String last(int count) {
    return mRead.substring(mRead.length() - count, mRead.length());
  }
  
  private Token startToken(LexState state) {
    mStartLine = mLine;
    mStartCol = mCol;
    
    mRead = "";
    
    changeToken(state);
    return null;
  }

  private Token changeToken(LexState state) {
    mState = state;
    advance();
    return null;
  }
  
  private Token characterToken(TokenType type) {
    return new Token(lastCharacterPosition(), type, last(1));
  }

  private Token createStringToken(TokenType type) {
    String text = mRead;
    mState = LexState.DEFAULT;
    
    Position position = currentPosition();
    
    // Handle reserved words.
    if (text.equals("false")) return new Token(position, TokenType.BOOL, false);
    if (text.equals("true")) return new Token(position, TokenType.BOOL, true);
    if (text.equals("=")) return new Token(position, TokenType.EQUALS);
    if (text.equals("->")) return new Token(position, TokenType.ARROW);
    
    return new Token(position, type, text);
  }

  private Token createIntToken(TokenType type) {
    int value = Integer.parseInt(mRead);
    mState = LexState.DEFAULT;
    return new Token(currentPosition(), type, value);
  }

  private Token createDoubleToken(TokenType type) {
    double value = Double.parseDouble(mRead);
    mState = LexState.DEFAULT;
    return new Token(currentPosition(), type, value);
  }

  private Position currentPosition() {
    return new Position(mSourceFile, mStartLine, mStartCol, mLine, mCol);
  }
  
  private Position lastCharacterPosition() {
    return new Position(mSourceFile, mLine, mCol - 1, mLine, mCol);
  }
  
  private Token advance() {
    return advance(true);
  }
  
  private Token advance(boolean addToRead) {
    if (mText.current() == '\0') {
      return new Token(Position.none(), TokenType.EOF);
    }
    
    if (addToRead) {
      mRead += mText.current();
    }
    
    // Update the position.
    if (mText.current() == '\n') {
      mLine++;
      mCol = 1;
    } else {
      mCol++;
    }
    
    mText.advance();

    return null;
  }
  
  private void advance(int count) {
    for (int i = 0; i < count; i++) {
      advance();
    }
  }
  
  private boolean isAlpha(final char c) {
    return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
        || (c == '_');
  }

  private boolean isDigit(final char c) {
    return (c >= '0') && (c <= '9');
  }

  private boolean isOperator(final char c) {
    return "~!$%^&*-=+|/?<>".indexOf(c) != -1;
  }

  private enum LexState {
    DEFAULT,
    IN_NAME,
    IN_OPERATOR,
    IN_NUMBER,
    IN_DECIMAL,
    IN_FRACTION,
    IN_MINUS,
    IN_STRING,
    IN_STRING_ESCAPE,
    IN_TYPE_PARAM,
    IN_LINE_COMMENT,
    IN_BLOCK_COMMENT,
    IN_LINE_CONTINUATION
  }

  private final String mSourceFile;
  private final CharacterReader mText;
  private String mRead;
  private LexState mState;
  private int mStartLine;
  private int mStartCol;
  private boolean mEatLines;
  private int mLine;
  private int mCol;
}
