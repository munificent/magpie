#pragma once

#include "Macros.h"
#include "Token.h"

namespace magpie
{
  class Memory;
  
  class Lexer {
  public:
    Lexer(gc<String> source)
    : source_(source),
      pos_(0),
      start_(0) {}
    
    // Lexes and returns the next full Token read from the source.
    temp<Token> readToken();
    
  private:
    bool isDone() const;
    
    bool isWhitespace(char c) const;
    bool isNameStart(char c) const;
    bool isName(char c) const;
    bool isDigit(char c) const;

    char peek(int ahead = 0) const;
    
    char advance();
    
    temp<Token> makeToken(TokenType type);

    void skipLineComment();

    temp<Token> readName();
    temp<Token> readNumber();

    gc<String> source_;
    bool    needsLine_;
    int     pos_;
    int     start_;
    
    NO_COPY(Lexer);
  };
}

