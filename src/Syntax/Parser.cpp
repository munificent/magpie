#include <sstream>

#include "Lexer.h"
#include "Node.h"
#include "Parser.h"

namespace magpie
{
  Parser::PrefixParseFn Parser::prefixParsers_[] = {
    // Punctuators.
    NULL,                 // TOKEN_LEFT_PAREN
    NULL,                 // TOKEN_RIGHT_PAREN
    NULL,                 // TOKEN_LEFT_BRACKET
    NULL,                 // TOKEN_RIGHT_BRACKET
    NULL,                 // TOKEN_LEFT_BRACE
    NULL,                 // TOKEN_RIGHT_BRACE
    NULL,                 // TOKEN_PLUS
    NULL,                 // TOKEN_MINUS
    NULL,                 // TOKEN_STAR
    NULL,                 // TOKEN_SLASH

    // Keywords.
    NULL,                 // TOKEN_CASE
    NULL,                 // TOKEN_DEF
    NULL,                 // TOKEN_DO
    NULL,                 // TOKEN_ELSE
    NULL,                 // TOKEN_FOR
    NULL,                 // TOKEN_IF
    NULL,                 // TOKEN_IS
    NULL,                 // TOKEN_MATCH
    NULL,                 // TOKEN_RETURN
    NULL,                 // TOKEN_THEN
    NULL,                 // TOKEN_WHILE

    NULL,                 // TOKEN_NAME
    &Parser::number,      // TOKEN_NUMBER
    NULL,                 // TOKEN_STRING

    NULL,                 // TOKEN_LINE
    NULL,                 // TOKEN_ERROR
    NULL                  // TOKEN_EOF
  };

  Parser::InfixParseFn Parser::infixParsers_[] = {
    // Punctuators.
    NULL,                 // TOKEN_LEFT_PAREN
    NULL,                 // TOKEN_RIGHT_PAREN
    NULL,                 // TOKEN_LEFT_BRACKET
    NULL,                 // TOKEN_RIGHT_BRACKET
    NULL,                 // TOKEN_LEFT_BRACE
    NULL,                 // TOKEN_RIGHT_BRACE
    &Parser::binaryOp,    // TOKEN_PLUS
    NULL,                 // TOKEN_MINUS
    NULL,                 // TOKEN_STAR
    NULL,                 // TOKEN_SLASH

    // Keywords.
    NULL,                 // TOKEN_CASE
    NULL,                 // TOKEN_DEF
    NULL,                 // TOKEN_DO
    NULL,                 // TOKEN_ELSE
    NULL,                 // TOKEN_FOR
    NULL,                 // TOKEN_IF
    NULL,                 // TOKEN_IS
    NULL,                 // TOKEN_MATCH
    NULL,                 // TOKEN_RETURN
    NULL,                 // TOKEN_THEN
    NULL,                 // TOKEN_WHILE

    NULL,                 // TOKEN_NAME
    NULL,                 // TOKEN_NUMBER
    NULL,                 // TOKEN_STRING

    NULL,                 // TOKEN_LINE
    NULL,                 // TOKEN_ERROR
    NULL                  // TOKEN_EOF
  };

  // TODO(bob): Figure out full precedence table.
  int Parser::infixPrecedences_[] = {
    // Punctuators.
    -1, // TOKEN_LEFT_PAREN
    -1, // TOKEN_RIGHT_PAREN
    -1, // TOKEN_LEFT_BRACKET
    -1, // TOKEN_RIGHT_BRACKET
    -1, // TOKEN_LEFT_BRACE
    -1, // TOKEN_RIGHT_BRACE
    4,  // TOKEN_PLUS
    -1, // TOKEN_MINUS
    -1, // TOKEN_STAR
    -1, // TOKEN_SLASH

    // Keywords.
    -1, // TOKEN_CASE
    -1, // TOKEN_DEF
    -1, // TOKEN_DO
    -1, // TOKEN_ELSE
    -1, // TOKEN_FOR
    -1, // TOKEN_IF
    -1, // TOKEN_IS
    -1, // TOKEN_MATCH
    -1, // TOKEN_RETURN
    -1, // TOKEN_THEN
    -1, // TOKEN_WHILE

    -1, // TOKEN_NAME
    -1, // TOKEN_NUMBER
    -1, // TOKEN_STRING

    -1, // TOKEN_LINE
    -1, // TOKEN_ERROR
    -1  // TOKEN_EOF
  };

  temp<Node> Parser::parseExpression(int precedence)
  {
    // Pratt operator precedence parser. See this for more:
    // http://journal.stuffwithstuff.com/2011/03/19/pratt-parsers-expression-parsing-made-easy/

    temp<Token> token = consume();
    PrefixParseFn prefix = prefixParsers_[token->type()];

    if (prefix == NULL)
    {
      // TODO(bob): Report error better.
      std::cout << "No prefix parser for " << token << "." << std::endl;
      return temp<Token>();
    }

    temp<Node> left = (this->*prefix)(token);

    while (precedence < infixPrecedences_[current().type()])
    {
      token = consume();

      InfixParseFn infix = infixParsers_[token->type()];
      left = (this->*infix)(left, token);
    }

    return left;

    // TODO(bob): Implement!
    return temp<Node>();
  }

  temp<Node> Parser::number(temp<Token> token)
  {
    double number = atof(token->text().cString());
    return NumberNode::create(number);
  }

  temp<Node> Parser::binaryOp(temp<Node> left, temp<Token> token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    temp<Node> right = parseExpression(infixPrecedences_[token->type()]);
    
    return BinaryOpNode::create(left, token->type(), right);
  }

  const Token& Parser::current()
  {
    fillLookAhead(1);
    return *read_[0];
  }

  bool Parser::lookAhead(TokenType type)
  {
    fillLookAhead(1);
    return read_[0]->type() == type;
  }

  bool Parser::lookAhead(TokenType current, TokenType next)
  {
    fillLookAhead(2);
    return read_[0]->is(current) && read_[1]->is(next);
  }

  bool Parser::match(TokenType type)
  {
    if (lookAhead(type))
    {
      consume();
      return true;
    }
    else
    {
      return false;
    }
  }

  void Parser::expect(TokenType expected, const char* errorMessage)
  {
    if (!lookAhead(expected)) error(errorMessage);
  }

  temp<Token> Parser::consume()
  {
    fillLookAhead(1);
    return read_.dequeue();
  }

  temp<Token> Parser::consume(TokenType expected, const char* errorMessage)
  {
    if (lookAhead(expected)) return consume();

    error(errorMessage);
    return temp<Token>();
  }

  void Parser::error(const char* message)
  {
    hadError_ = true;
    // TODO(bob): Report error instead of printing it directly.
    std::stringstream error;
    std::cout << "Parse error on '" << current() << "': " << message;
  }

  void Parser::fillLookAhead(int count)
  {
    while (read_.count() < count) read_.enqueue(lexer_.readToken());
  }
}

