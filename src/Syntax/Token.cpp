#include "Token.h"
#include "Memory.h"

namespace magpie
{
  temp<Token> Token::create(TokenType type, const gc<String> text)
  {
    return Memory::makeTemp(new Token(type, text));
  }
  
  const char* Token::typeString(TokenType type)
  {
    switch (type)
    {
        // Punctuators.
      case TOKEN_LEFT_PAREN:    return "(";
      case TOKEN_RIGHT_PAREN:   return ")";
      case TOKEN_LEFT_BRACKET:  return "[";
      case TOKEN_RIGHT_BRACKET: return "]";
      case TOKEN_LEFT_BRACE:    return "{";
      case TOKEN_RIGHT_BRACE:   return "}";
      case TOKEN_PLUS:          return "+";
      case TOKEN_MINUS:         return "-";
      case TOKEN_STAR:          return "*";
      case TOKEN_SLASH:         return "/";
      case TOKEN_PERCENT:       return "%";
        
        // Keywords.
      case TOKEN_AND:           return "and";
      case TOKEN_CASE:          return "case";
      case TOKEN_DEF:           return "def";
      case TOKEN_DO:            return "do";
      case TOKEN_ELSE:          return "else";
      case TOKEN_FALSE:         return "false";
      case TOKEN_FOR:           return "for";
      case TOKEN_IF:            return "if";
      case TOKEN_IS:            return "is";
      case TOKEN_MATCH:         return "match";
      case TOKEN_NOT:           return "not";
      case TOKEN_OR:            return "or";
      case TOKEN_RETURN:        return "return";
      case TOKEN_THEN:          return "then";
      case TOKEN_TRUE:          return "true";
      case TOKEN_VAL:           return "val";
      case TOKEN_VAR:           return "var";
      case TOKEN_WHILE:         return "while";
      case TOKEN_XOR:           return "xor";
        
      case TOKEN_NAME:          return "name";
      case TOKEN_NUMBER:        return "number";
      case TOKEN_STRING:        return "string";
        
      case TOKEN_LINE:          return "line";
      case TOKEN_ERROR:         return "error";
      case TOKEN_EOF:           return "eof";
      default:
        ASSERT(false, "Unknown TokenType.");
    }
  }
  
  Token::Token(TokenType type, const gc<String> text)
  : type_(type),
    text_(text)
  {}
  
  void Token::reach()
  {
    Memory::reach(text_);
  }
  
  void Token::trace(std::ostream& out) const
  {
    out << "token " << Token::typeString(type_) << " " << text_;
  }
}
