package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.Token;
import com.stuffwithstuff.magpie.TokenType;

public class Lexer {

  public Lexer(String text) {
    mText = text;
    mState = LexState.DEFAULT;
    mIndex = 0;
    mTokenStart = 0;

    // ignore starting lines
    mEatLines = true;
  }

  public Token readToken() {
    while (true) {
      Token token = readRawToken();

      switch (token.getType()) {
      // ignore lines after tokens that can't end an expression
      case LEFT_PAREN:
      case LEFT_BRACKET:
      case LEFT_BRACE:
      case COMMA:
      case DOT:
      case OPERATOR:
        mEatLines = true;
        return token;

      case LINE:
        if (!mEatLines) {
          // collapse multiple lines
          mEatLines = true;
          return token;
        }
        break;

      default:
        // a line after any other token is significant
        mEatLines = false;
        return token;
      }
    }
  }

  private Token readRawToken() {
    while (mIndex <= mText.length()) {

      // tack on a '\0' to the end of the string and lex it. that will let
      // us conveniently have a place to end any token that goes to the
      // end of the string
      char c = (mIndex < mText.length()) ? mText.charAt(mIndex) : '\0';

      switch (mState) {
      case DEFAULT:
        switch (c) {
        case '(':
          return singleCharToken(TokenType.LEFT_PAREN);
        case ')':
          return singleCharToken(TokenType.RIGHT_PAREN);
        case '[':
          return singleCharToken(TokenType.LEFT_BRACKET);
        case ']':
          return singleCharToken(TokenType.RIGHT_BRACKET);
        case '{':
          return singleCharToken(TokenType.LEFT_BRACE);
        case '}':
          return singleCharToken(TokenType.RIGHT_BRACE);
        case ',':
          return singleCharToken(TokenType.COMMA);
        case ';':
          return singleCharToken(TokenType.LINE);
        case '.':
          return singleCharToken(TokenType.DOT);

        case '"':
          startToken(LexState.IN_STRING);
          break;
        case '#':
          startToken(LexState.IN_COMMENT);
          break;

        case '-':
          startToken(LexState.IN_MINUS);
          break;

        case '\n':
        case '\r':
          return singleCharToken(TokenType.LINE);

          // ignore whitespace
        case ' ':
        case '\t':
        case '\0':
          mIndex++;
          break;

        default:
          if (isAlpha(c)) {
            startToken(LexState.IN_NAME);
          } else if (isOperator(c)) {
            startToken(LexState.IN_OPERATOR);
          } else if (isDigit(c)) {
            startToken(LexState.IN_NUMBER);
          } else {
            // ### bob: hack temp. unexpected character
            return new Token(TokenType.EOF);
          }
          break;
        }
        break;

      case IN_NAME:
        if (isAlpha(c) || isDigit(c) || isOperator(c)) {
          mIndex++;
        } else {
          return createStringToken(TokenType.NAME);
        }
        break;

      case IN_OPERATOR:
        if (isOperator(c) || isAlpha(c) || isDigit(c)) {
          mIndex++;
        } else {
          return createStringToken(TokenType.OPERATOR);
        }
        break;

      case IN_NUMBER:
        if (isDigit(c)) {
          mIndex++;
        } else if (c == '.') {
          changeToken(LexState.IN_DECIMAL);
        } else {
          return createIntToken(TokenType.INT);
        }
        break;

      case IN_DECIMAL:
        if (isDigit(c)) {
          mIndex++;
        } else {
          return createDoubleToken(TokenType.DOUBLE);
        }
        break;

      case IN_MINUS:
        if (isDigit(c)) {
          changeToken(LexState.IN_NUMBER);
        } else if (isOperator(c) || isAlpha(c)) {
          changeToken(LexState.IN_OPERATOR);
        } else {
          return createStringToken(TokenType.OPERATOR);
        }
        break;

      case IN_STRING:
        if (c == '"') {
          // eat the closing "
          mIndex++;

          // get the contained string without the quotes
          String text = mText.substring(mTokenStart + 1, mIndex - 1);
          mState = LexState.DEFAULT;
          return new Token(TokenType.STRING, text);
        } else if (c == '\0') {
          // ### bob: need error handling. ran out of characters before
          // string was closed
          return new Token(TokenType.EOF);
        } else {
          mIndex++;
        }
        break;

      case IN_COMMENT:
        if ((c == '\n') || (c == '\r')) {
          // don't eat the newline here. that way, a comment on the
          // same line as other code still allows the newline to be
          // processed
          mState = LexState.DEFAULT;
        } else {
          mIndex++;
        }
        break;
      }
    }

    return new Token(TokenType.EOF);
  }

  private Token singleCharToken(TokenType type) {
    mIndex++;
    return new Token(type, mText.substring(mIndex - 1, mIndex));
  }

  private void startToken(LexState state) {
    mTokenStart = mIndex;
    changeToken(state);
  }

  private void changeToken(LexState state) {
    mState = state;
    mIndex++;
  }

  private Token createStringToken(TokenType type) {
    String text = mText.substring(mTokenStart, mIndex);
    mState = LexState.DEFAULT;
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
    DEFAULT, IN_NAME, IN_OPERATOR, IN_NUMBER, IN_DECIMAL, IN_MINUS, IN_STRING, IN_COMMENT
  }

  private final String mText;
  private LexState mState;
  private int mTokenStart;
  private int mIndex;
  private boolean mEatLines;
}
