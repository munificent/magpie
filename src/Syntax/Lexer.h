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

    char peek(int ahead = 0) const;
    
    char advance();
    
    temp<Token> makeToken(TokenType type);
    
    /*
     bool isAlpha(char c) const;
     bool isDigit(char c) const;
     bool isOperator(char c) const;
    
    void skipBlockComment();
    temp<Token> readString();
    temp<Token> readNumber();
    temp<Token> readName();
    
    void advanceLine();
     */
    
    gc<String> source_;
    bool    needsLine_;
    int     pos_;
    int     start_;
    
    NO_COPY(Lexer);
  };
}

