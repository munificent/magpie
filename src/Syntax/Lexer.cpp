#include <iostream> // for debugging
#include <cstdlib>
#include <cstring>

#include "Lexer.h"

namespace magpie
{
  temp<Token> Lexer::readToken() {
    temp<String> text = String::create("1");
    return Token::create(TOKEN_NUMBER, text);
    /*
    while (true) {
      if (isDone()) return temp<Token>(new Token(TOKEN_EOF));
      
      start_ = pos_;
      
      char c = peek();
      switch (c) {
        case ' ':
        case '\t':
          // Skip whitespace.
          while (isWhitespace(peek())) advance();
          break;
          
        case '\0':
          // End of the line.
          mNeedsLine = true;
          return Ref<Token>(new Token(TOKEN_LINE));
          
        case '(': return singleToken(TOKEN_LEFT_PAREN);
        case ')': return singleToken(TOKEN_RIGHT_PAREN);
        case '[': return singleToken(TOKEN_LEFT_BRACKET);
        case ']': return singleToken(TOKEN_RIGHT_BRACKET);
        case '{': return singleToken(TOKEN_LEFT_BRACE);
        case '}': return singleToken(TOKEN_RIGHT_BRACE);
        case ',': return singleToken(TOKEN_LINE);
        case '@': return singleToken(TOKEN_AT);
        case '.': return singleToken(TOKEN_DOT);
        case '#': return singleToken(TOKEN_HASH);
        case ';': return singleToken(TOKEN_SEMICOLON);
        case '\\': return singleToken(TOKEN_IGNORE_LINE);
        case '|': return singleToken(TOKEN_PIPE);
          
        case ':':
          advance();
          if (peek() == ':') {
            // "::".
            advance();
            return Ref<Token>(new Token(TOKEN_BIND));
          }
          
          // Just a ":" by itself.
          return Ref<Token>(new Token(TOKEN_KEYWORD, ":"));
          
        case '-':
          advance();
          if (isDigit(peek())) return readNumber();
          return readOperator();
          
        case '/':
          advance();
          if (peek() == '/') {
            // Line comment, so ignore the rest of the line and
            // emit the line token.
            mNeedsLine = true;
            return Ref<Token>(new Token(TOKEN_LINE));
          } else if (peek() == '*') {
            skipBlockComment();
          } else {
            return readOperator();
          }
          break;
          
        case '"': return readString();
          
        default:
          if (isDigit(c)) return readNumber();
          if (isAlpha(c)) return readName();
          if (isOperator(c)) return readOperator();
          
          // If we got here, we don't know what it is. Just eat it so
          // we don't get stuck.
          advance();
          return Ref<Token>(new Token(TOKEN_ERROR, String::Format(
              "Unrecognized character \"%c\".", c)));
      }
    }
    */
  }
  /*

  bool Lexer::isDone() const {
    return mNeedsLine && mReader.EndOfLines();
  }
  
  bool Lexer::isWhitespace(char c) const {
    return (c == ' ') || (c == '\t');
  }
  
  bool Lexer::isAlpha(char c) const {
    return (c == '_') ||
          ((c >= 'a') && (c <= 'z')) ||
          ((c >= 'A') && (c <= 'Z'));
  }
  
  bool Lexer::isDigit(char c) const {
    return (c >= '0') && (c <= '9');
  }
  
  bool Lexer::isOperator(char c) const {
    return (c != '\0') &&
    (strchr("-+=/<>?~!$%^&*", c) != NULL);
  }
  
  char Lexer::peek(int ahead) const
  {
    if (pos_ + ahead >= source_->length()) return '\0';
    return (*source_)[pos_ + ahead];
  }
  
  char Lexer::advance()
  {
    char c = peek();
    pos_++;
    return c;
  }
  
  void Lexer::skipBlockComment()
  {
    advance();
    advance();
    
    int nesting = 1;
    
    while (nesting > 0) {
      // TODO(bob): Unterminated comment. Should return error.
      if (isDone()) return;
      
      if ((peek() == '/') && (peek(1) == '*')) {
        advance();
        advance();
        nesting++;
      } else if ((peek() == '*') && (peek(1) == '/')) {
        advance();
        advance();
        nesting--;
      } else if (peek() == '\0') {
        advanceLine();
      } else {
        advance();
      }
    }
  }
  
  Ref<Token> Lexer::singleToken(TokenType type) {
    advance();
    return Ref<Token>(new Token(type));
  }
  
  Ref<Token> Lexer::readString() {
    advance();
    
    String text;
    while (true) {
      if (isDone()) return Ref<Token>(new Token(TOKEN_ERROR, "Unterminated string."));
      
      char c = advance();
      if (c == '"') return Ref<Token>(new Token(TOKEN_STRING, text));
      
      // An escape sequence.
      if (c == '\\') {
        if (isDone()) return Ref<Token>(new Token(TOKEN_ERROR,
                                                  "Unterminated string escape."));
        
        char e = advance();
        switch (e) {
          case 'n': text += "\n"; break;
          case '"': text += "\""; break;
          case '\\': text += "\\"; break;
          case 't': text += "\t"; break;
          default:
            return Ref<Token>(new Token(TOKEN_ERROR, String::Format(
                                                                    "Unrecognized escape sequence \"%c\".", e)));
        }
      } else {
        // Normal character.
        text += c;
      }
    }
  }
  
  Ref<Token> Lexer::readNumber() {
    advance();
    while (isDigit(peek())) advance();
    
    // Read the fractional part, if any.
    if (peek() == '.') {
      advance();
      while (isDigit(peek())) advance();
    }
    
    String text = mLine.Substring(mStart, mPos - mStart);
    double number = atof(text.CString());
    return Ref<Token>(new Token(TOKEN_NUMBER, number));
  }
  
  Ref<Token> Lexer::readName() {
    while (isOperator(peek()) || isAlpha(peek()) || isDigit(peek())) {
      // Comments take priority over names.
      if ((peek() == '/') && (peek(1) == '/')) break;
      if ((peek() == '/') && (peek(1) == '*')) break;
      advance();
    }
    
    // If it ends in ":", it's a keyword.
    TokenType type = TOKEN_NAME;
    if (peek() == ':') {
      advance();
      type = TOKEN_KEYWORD;
    }
    
    String name = mLine.Substring(mStart, mPos - mStart);
    
    if (name == "return") return Ref<Token>(new Token(TOKEN_RETURN));
    if (name == "self") return Ref<Token>(new Token(TOKEN_SELF));
    if (name == "undefined") return Ref<Token>(new Token(TOKEN_UNDEFINED));
    
    return Ref<Token>(new Token(type, name));
  }
  */
}
