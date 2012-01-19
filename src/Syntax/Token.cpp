#include "Token.h"
#include "Memory.h"

namespace magpie {
  
  temp<Token> Token::create(AllocScope& scope, TokenType type,
                            const gc<String> text) {
    return scope.makeTemp(new(scope) Token(type, text));
  }
  
  Token::Token(TokenType type, const gc<String> text)
  : type_(type),
    text_(text) {}
}

