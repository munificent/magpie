#pragma once

#include "Macros.h"
#include "Token.h"

namespace magpie
{
  class Lexer {
  public:
    Lexer(gc<String> source)
    : source_(source),
      pos_(0),
      start_(0) {}
    
    // Lexes and returns the next full Token read from the source.
    temp<Token> readToken(AllocScope& scope);
    
  private:
    /*
    bool isDone() const;
    
    char peek(int ahead = 0) const;
    
    char advance();
    
    void skipBlockComment();
    temp<Token> singleToken(TokenType type);
    temp<Token> readString();
    temp<Token> readNumber();
    temp<Token> readName();
    
    void advanceLine();
    
    bool isWhitespace(char c) const;
    bool isAlpha(char c) const;
    bool isDigit(char c) const;
    bool isOperator(char c) const;
     */
    
    gc<String> source_;
    bool    needsLine_;
    int     pos_;
    int     start_;
    
    NO_COPY(Lexer);
  };
}

