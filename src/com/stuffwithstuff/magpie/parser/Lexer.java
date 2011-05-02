package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.parser.Token;
import com.stuffwithstuff.magpie.parser.TokenType;

public class Lexer implements TokenReader {

  public Lexer(CharacterReader text) {
    mText = text;
    mLine = 1;
    mCol = 1;
    
    mStartLine = 1;
    mStartCol = 1;
    mRead = "";
  }

  public Token readToken() {
    char c = advance();
    switch (c) {
      // Whitespace.
    case ' ':
    case '\t':
      return readWhitespace();

      // Punctuators.
    case '(': return makeToken(TokenType.LEFT_PAREN);
    case ')': return makeToken(TokenType.RIGHT_PAREN);
    case '[': return makeToken(TokenType.LEFT_BRACKET);
    case ']': return makeToken(TokenType.RIGHT_BRACKET);
    case '{': return makeToken(TokenType.LEFT_BRACE);
    case '}': return makeToken(TokenType.RIGHT_BRACE);
    case '`': return makeToken(TokenType.BACKTICK);
    case ',': return makeToken(TokenType.COMMA);

      // Match line ending characters.
    case ';':
    case '\n':
    case '\r':
      return makeToken(TokenType.LINE);

      // Strings.
    case '"':
      return readString();
      
      // Comments.
    case '/':
      switch (peek()) {
      case '/': return readLineComment();
      case '*': return readBlockComment();
      default:  return readName();
      }
      
      // Need to handle numeric literals.
    case '-':
      if (isDigit(peek())) {
        // A negative number.
        return readNumber();
      } else if (isName(peek())) {
        // A name starting with "-".
        return readName();
      } else {
        // A "-" by itself.
        return makeToken(TokenType.NAME);
      }
      
    case '\\':
      return makeToken(TokenType.LINE_CONTINUATION);
      
      // EOF.
    case '\0': return makeToken(TokenType.EOF);
    
    default:
      if (isName(c)) {
        // Identifier.
        return readName();
      } else if (isDigit(c)) {
        // Number.
        return readNumber();
      } else {
        throw new ParseException("Unknown character.");
      }
    }
  }
  
  private Token readWhitespace() {
    while (true) {
      switch (peek()) {
      case ' ':
      case '\t':
        advance();
        break;
        
      default:
        return makeToken(TokenType.WHITESPACE);
      }
    }
  }
  
  private Token readString() {
    while (true) {
      switch (advance()) {
      case '\\':
        // String escape.
        switch (advance()) {
        case 'n':
        case 't':
        case '"':
        case '\\':
          // Do nothing, valid escape.
          break;
          
        default: throw new ParseException("Unknown string escape.");
        }
        break;
        
      case '"':
        return makeToken(TokenType.STRING, convertStringLiteral(mRead));
       
      case '\0': throw new ParseException("Unterminated string.");
      
      default:
        // Do nothing, already advanced.
      }
    }
  }
  
  private Token readLineComment() {
    advance(); // Consume second "/".
    
    // See if it's a "///" doc comment.
    boolean isDoc = (peek() == '/');
      
    while (true) {
      switch (peek()) {
      case '\n':
      case '\r':
      case '\0':
        String value = mRead.substring(isDoc ? 3 : 2).trim();
        return makeToken(
            isDoc ? TokenType.DOC_COMMENT : TokenType.LINE_COMMENT, value);
        
      default:
        advance();
      }
    }
  }

  private Token readBlockComment() {
    while (true) {
      switch (advance()) {
      case '*':
        switch (advance()) {
        case '/': return makeToken(TokenType.BLOCK_COMMENT);
        case '\0': throw new ParseException("Unterminated block comment.");
        default: // Do nothing, keep advancing.
        }
        break;
        
      case '\0': throw new ParseException("Unterminated block comment.");
      default: // Do nothing, keep advancing.
      }
    }
  }

  private Token readName() {
    while (true) {
      if (isName(peek()) || isDigit(peek())) {
        advance();
      } else if (peek() == ':') {
        advance();
        
        // Trim off the ":".
        String value = mRead.substring(0, mRead.length() - 1);
        return makeToken(TokenType.FIELD, value);
      } else {
        return makeToken(TokenType.NAME);
      }
    }
  }
  
  private Token readNumber() {
    while (true) {
      if (isDigit(peek())) {
        advance();
      } else {
        return makeToken(TokenType.INT, Integer.parseInt(mRead));
      }
    }
  }
  
  private String convertStringLiteral(String literal) {
    // Trim the quotes and convert the escapes.
    StringBuilder builder = new StringBuilder();
    
    boolean inEscape = false;
    for (int i = 1; i < literal.length() - 1; i++) {
      if (inEscape) {
        switch (literal.charAt(i)) {
        case 'n': builder.append("\n"); break;
        case 't': builder.append("\t"); break;
        case '\\': builder.append("\\"); break;
        case '"': builder.append("\""); break;
        }
        inEscape = false;
      } else {
        if (literal.charAt(i) == '\\') {
          inEscape = true;
        } else {
          builder.append(literal.charAt(i));
        }
      }
    }

    return builder.toString();
  }
  
  private char peek() {
    return mText.current();
  }
  
  private char advance() {
    char c = mText.current();
    mText.advance();

    mRead += c;

    // Update the position.
    if (c == '\n') {
      mLine++;
      mCol = 1;
    } else {
      mCol++;
    }

    return c;
  }

  private Token makeToken(TokenType type) {
    return makeToken(type, mRead);
  }
  
  private Token makeToken(TokenType type, Object value) {
    // Handle reserved words.
    if (type == TokenType.NAME) {
      if (mRead.equals("nothing")) {
        type = TokenType.NOTHING;
      } else if (mRead.equals("false")) {
        type = TokenType.BOOL;
        value = false;
      } else if (mRead.equals("true")) {
        type = TokenType.BOOL;
        value = true;
      } else if (mRead.equals("=")) {
        type = TokenType.EQUALS;
      }
    }
    
    Token token = new Token(currentPosition(), type, mRead, value);
    
    mStartLine = mLine;
    mStartCol = mCol;
    mRead = "";
    
    return token;
  }
  
  private Position currentPosition() {
    return new Position(mText.getDescription(),
        mStartLine, mStartCol, mLine, mCol);
  }
  
  private boolean isDigit(final char c) {
    return (c >= '0') && (c <= '9');
  }
  
  private boolean isName(final char c) {
    return ((c >= 'a') && (c <= 'z'))
        || ((c >= 'A') && (c <= 'Z'))
        || (c == '_')
        || ("~!$%^&*-=+|/?<>.".indexOf(c) != -1);
  }
  
  private final CharacterReader mText;
  private String mRead;
  private int mStartLine;
  private int mStartCol;
  private int mLine;
  private int mCol;
}
