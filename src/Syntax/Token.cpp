#include "Token.h"
#include "Memory.h"

namespace magpie
{
  temp<Token> Token::create(TokenType type, const gc<String> text)
  {
    return Memory::makeTemp(new Token(type, text));
  }

  Token::Token(TokenType type, const gc<String> text)
  : type_(type),
    text_(text) {}

  std::ostream& operator <<(std::ostream& out, const Token& right)
  {
    out << right.type() << " " << right.text();
    return out;
  };
}
