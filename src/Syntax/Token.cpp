#include "Token.h"
#include "Memory.h"

namespace magpie {
  
  temp<Token> Token::create(TokenType type, const gc<String> text) {
    return Memory::makeTemp(new Token(type, text));
  }
  
  Token::Token(TokenType type, const gc<String> text)
  : type_(type),
    text_(text) {}
}

