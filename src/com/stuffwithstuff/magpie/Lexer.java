package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.Token;
import com.stuffwithstuff.magpie.TokenType;

public class Lexer {

  public Lexer(String text) {
    mText = text;
    mState = LexState.DEFAULT;
    mIndex = 0;
    mTokenStart = 0;

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
    case LEFT_PAREN:
    case LEFT_BRACKET:
    case LEFT_BRACE:
    case COMMA:
    case DOT:
    case OPERATOR:
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
    // Tack on a '\0' to the end of the string and lex it. That will let
    // us conveniently have a place to end any token that goes to the
    // end of the string.
    char c = (mIndex < mText.length()) ? mText.charAt(mIndex) : '\0';

    switch (mState) {
    case DEFAULT:
      if (match("(")) return new Token(TokenType.LEFT_PAREN, last(1));
      if (match(")")) return new Token(TokenType.RIGHT_PAREN, last(1));
      if (match("[")) return new Token(TokenType.LEFT_BRACKET, last(1));
      if (match("]")) return new Token(TokenType.RIGHT_BRACKET, last(1));
      if (match("{")) return new Token(TokenType.LEFT_BRACE, last(1));
      if (match("}")) return new Token(TokenType.RIGHT_BRACE, last(1));
      if (match(",")) return new Token(TokenType.COMMA, last(1));
      if (match(".")) return new Token(TokenType.DOT, last(1));

      // Match line ending characters.
      if (match(";"))  return new Token(TokenType.LINE, last(1));
      if (match("\n")) return new Token(TokenType.LINE, last(1));
      if (match("\r")) return new Token(TokenType.LINE, last(1));
      
      // Comments.
      if (match("//")) return startToken(LexState.IN_LINE_COMMENT);
      if (match("/*")) return startToken(LexState.IN_BLOCK_COMMENT);
      
      if (lookAhead("\"")) return startToken(LexState.IN_STRING);
      if (lookAhead("-")) return startToken(LexState.IN_MINUS);
      
      if (isAlpha(c)) return startToken(LexState.IN_NAME);
      if (isOperator(c)) return startToken(LexState.IN_OPERATOR);
      if (isDigit(c)) return startToken(LexState.IN_NUMBER);
      if (isAlpha(c)) return startToken(LexState.IN_NAME);

      // Ignore whitespace.
      if (match(" ")) return null;
      if (match("\t")) return null;
      
      // TODO(bob): Hack temp. Unexpected character
      return new Token(TokenType.EOF);

    case IN_NAME:
      if (isAlpha(c) || isDigit(c) || isOperator(c)) {
        mIndex++;
        return null;
      } else {
        return createStringToken(TokenType.NAME);
      }

    case IN_OPERATOR:
      if (isOperator(c) || isAlpha(c) || isDigit(c)) {
        mIndex++;
        return null;
      } else {
        return createStringToken(TokenType.OPERATOR);
      }

    case IN_NUMBER:
      if (isDigit(c)) {
        mIndex++;
        return null;
      } else if (c == '.') {
        return changeToken(LexState.IN_DECIMAL);
      } else {
        return createIntToken(TokenType.INT);
      }

    case IN_DECIMAL:
      if (isDigit(c)) {
        return changeToken(LexState.IN_FRACTION);
      } else {
        // Rollback to reprocess the "." as its own token. This lets us parse
        // things like "123.foo".
        mIndex--;
        return createIntToken(TokenType.INT);
      }
      
    case IN_FRACTION:
      if (isDigit(c)) {
        mIndex++;
      } else {
        return createDoubleToken(TokenType.DOUBLE);
      }
      
    case IN_MINUS:
      if (isDigit(c)) {
        return changeToken(LexState.IN_NUMBER);
      } else if (isOperator(c) || isAlpha(c)) {
        return changeToken(LexState.IN_OPERATOR);
      } else {
        return createStringToken(TokenType.OPERATOR);
      }

    case IN_STRING:
      mIndex++;

      if (last(1).equals("\"")) {
        // Get the contained string without the quotes.
        String text = mText.substring(mTokenStart + 1, mIndex - 1);
        mState = LexState.DEFAULT;
        return new Token(TokenType.STRING, text);
      } else {
        // Consume other characters.
        return null;
      }

    case IN_LINE_COMMENT:
      if (match("\n") || match("\r")) {
        mState = LexState.DEFAULT;
        return new Token(TokenType.LINE, last(1));
      } else {
        // Ignore everything else.
        mIndex++;
        return null;
      }
      
    default:
      throw new Error("Unexpected lex state.");
    }
  }
  
  private boolean lookAhead(String text) {
    // See if all of the characters match.
    for (int i = 0; i < text.length(); i++) {
      if (mIndex + i >= mText.length()) return false;
      if (mText.charAt(mIndex + i) != text.charAt(i)) return false;
    }
    
    // If we got here, they did.
    return true;
  }

  private boolean match(String text) {
    boolean matched = lookAhead(text);
    
    if (matched) mIndex += text.length();
    return matched;
  }
  
  private String last(int count) {
    return mText.substring(mIndex - count, mIndex);
  }
  
  private Token startToken(LexState state) {
    mTokenStart = mIndex;
    changeToken(state);
    return null;
  }

  private Token changeToken(LexState state) {
    mState = state;
    mIndex++;
    return null;
  }

  private Token createStringToken(TokenType type) {
    String text = mText.substring(mTokenStart, mIndex);
    mState = LexState.DEFAULT;
    
    // Handle reserved words.
    if (text.equals("true")) return new Token(TokenType.BOOL, true);
    if (text.equals("false")) return new Token(TokenType.BOOL, false);
    if (text.equals("case")) return new Token(TokenType.CASE);
    if (text.equals("class")) return new Token(TokenType.CLASS);
    if (text.equals("def")) return new Token(TokenType.DEF);
    if (text.equals("do")) return new Token(TokenType.DO);
    if (text.equals("else")) return new Token(TokenType.ELSE);
    if (text.equals("end")) return new Token(TokenType.END);
    if (text.equals("fn")) return new Token(TokenType.FN);
    if (text.equals("for")) return new Token(TokenType.FOR);
    if (text.equals("if")) return new Token(TokenType.IF);
    if (text.equals("let")) return new Token(TokenType.LET);
    if (text.equals("match")) return new Token(TokenType.MATCH);
    if (text.equals("then")) return new Token(TokenType.THEN);
    if (text.equals("var")) return new Token(TokenType.VAR);
    if (text.equals("while")) return new Token(TokenType.WHILE);
    if (text.equals("=")) return new Token(TokenType.EQUALS);
    if (text.equals("->")) return new Token(TokenType.ARROW);
    
    return new Token(type, text);
  }

  private Token createIntToken(TokenType type) {
    String text = mText.substring(mTokenStart, mIndex);
    int value = Integer.parseInt(text);
    mState = LexState.DEFAULT;
    return new Token(type, value);
  }

  private Token createDoubleToken(TokenType type) {
    String text = mText.substring(mTokenStart, mIndex);
    double value = Double.parseDouble(text);
    mState = LexState.DEFAULT;
    return new Token(type, value);
  }

  private boolean isAlpha(final char c) {
    return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
        || (c == '_') || (c == '\'');
  }

  private boolean isDigit(final char c) {
    return (c >= '0') && (c <= '9');
  }

  private boolean isOperator(final char c) {
    return "`~!@#$%^&*-=+\\|/?<>".indexOf(c) != -1;
  }

  private enum LexState {
    DEFAULT, IN_NAME, IN_OPERATOR, IN_NUMBER, IN_DECIMAL, IN_FRACTION, IN_MINUS,
    IN_STRING, IN_LINE_COMMENT, IN_BLOCK_COMMENT
  }

  private final String mText;
  private LexState mState;
  private int mTokenStart;
  private int mIndex;
  private boolean mEatLines;
}
