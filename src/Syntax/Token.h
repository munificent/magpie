#pragma once

#include <iostream>

#include "Macros.h"
#include "MagpieString.h"

namespace magpie {
  class Memory;
  
  // The different types of Tokens that make up Magpie source code.
  enum TokenType {
    TOKEN_LEFT_PAREN,
    TOKEN_RIGHT_PAREN,
    TOKEN_LEFT_BRACKET,
    TOKEN_RIGHT_BRACKET,
    TOKEN_LEFT_BRACE,
    TOKEN_RIGHT_BRACE,

    TOKEN_RETURN,
    
    TOKEN_NAME,

    TOKEN_NUMBER,
    TOKEN_STRING,

    TOKEN_ERROR,
    TOKEN_EOF
  };

  // A single meaningful Token of source code. Generated by the Lexer, and
  // consumed by the Parser.
  class Token : public Managed {
  public:
    static temp<Token> create(AllocScope& scope, TokenType type,
                              gc<String> text);

    virtual size_t allocSize() const { return sizeof(Token); }

    TokenType     type()   const { return type_; }
    const String& text()   const { return *text_; }
  private:
    Token(TokenType type, const gc<String> text);
    
    TokenType   type_;
    gc<String>  text_;
  };
}

