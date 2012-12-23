#pragma once

#include "Macros.h"
#include "Token.h"

namespace magpie
{
  class Memory;

  class Lexer
  {
  public:
    // TODO(bob): Need to do something better for the strings here. Right now,
    // the lexer isn't a root.
    Lexer(gc<SourceFile> source)
    : source_(source),
      pos_(0),
      start_(0),
      skipNewline_(true),
      startRow_(1),
      startCol_(1),
      currentRow_(1),
      currentCol_(1)
    {}

    // Lexes and returns the next full Token read from the source. Handles
    // eliding newlines that should ignored.
    gc<Token> readToken();

  private:
    // Reads a single token without and newline processing.
    gc<Token> readRawToken();
    
    bool isDone() const;

    bool isWhitespace(char c) const;
    bool isNameStart(char c) const;
    bool isName(char c) const;
    bool isOperator(char c) const;
    bool isDigit(char c) const;

    char peek(int ahead = 0) const;
    bool match(char c);
    char advance();

    gc<Token> makeToken(TokenType type);
    gc<Token> makeToken(TokenType type, gc<String> text);
    gc<Token> error(gc<String> message);
    
    void skipLineComment();
    void skipBlockComment();

    gc<Token> readName();
    gc<Token> readNumber();
    gc<Token> readOperator();
    gc<Token> readString();

    gc<SourceFile> source_;
    int     pos_;
    int     start_;
    bool    skipNewline_;
    int     startRow_;
    int     startCol_;
    int     currentRow_;
    int     currentCol_;
    
    NO_COPY(Lexer);
  };
}

