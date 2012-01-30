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
    NULL,                 // TOKEN_PERCENT

    // Keywords.
    NULL,                 // TOKEN_AND
    NULL,                 // TOKEN_CASE
    NULL,                 // TOKEN_DEF
    NULL,                 // TOKEN_DO
    NULL,                 // TOKEN_ELSE
    &Parser::boolean,     // TOKEN_FALSE
    NULL,                 // TOKEN_FOR
    &Parser::ifThenElse,  // TOKEN_IF
    NULL,                 // TOKEN_IS
    NULL,                 // TOKEN_MATCH
    NULL,                 // TOKEN_NOT
    NULL,                 // TOKEN_OR
    NULL,                 // TOKEN_RETURN
    NULL,                 // TOKEN_THEN
    &Parser::boolean,     // TOKEN_TRUE
    NULL,                 // TOKEN_WHILE
    NULL,                 // TOKEN_XOR

    NULL,                 // TOKEN_NAME
    &Parser::number,      // TOKEN_NUMBER
    NULL,                 // TOKEN_STRING

    NULL,                 // TOKEN_LINE
    NULL,                 // TOKEN_ERROR
    NULL                  // TOKEN_EOF
  };

  // TODO(bob): Figure out full precedence table.
  Parser::InfixParser Parser::infixParsers_[] = {
    // Punctuators.
    { NULL, -1 },                 // TOKEN_LEFT_PAREN
    { NULL, -1 },                 // TOKEN_RIGHT_PAREN
    { NULL, -1 },                 // TOKEN_LEFT_BRACKET
    { NULL, -1 },                 // TOKEN_RIGHT_BRACKET
    { NULL, -1 },                 // TOKEN_LEFT_BRACE
    { NULL, -1 },                 // TOKEN_RIGHT_BRACE
    { &Parser::binaryOp, 7 },     // TOKEN_PLUS
    { &Parser::binaryOp, 7 },     // TOKEN_MINUS
    { &Parser::binaryOp, 8 },     // TOKEN_STAR
    { &Parser::binaryOp, 8 },     // TOKEN_SLASH
    { &Parser::binaryOp, 8 },     // TOKEN_PERCENT

    // Keywords.
    { &Parser::binaryOp, 3 },     // TOKEN_AND
    { NULL, -1 },                 // TOKEN_CASE
    { NULL, -1 },                 // TOKEN_DEF
    { NULL, -1 },                 // TOKEN_DO
    { NULL, -1 },                 // TOKEN_ELSE
    { NULL, -1 },                 // TOKEN_FALSE
    { NULL, -1 },                 // TOKEN_FOR
    { NULL, -1 },                 // TOKEN_IF
    { NULL, -1 },                 // TOKEN_IS
    { NULL, -1 },                 // TOKEN_MATCH
    { NULL, -1 },                 // TOKEN_NOT
    { &Parser::binaryOp, 3 },     // TOKEN_OR
    { NULL, -1 },                 // TOKEN_RETURN
    { NULL, -1 },                 // TOKEN_THEN
    { NULL, -1 },                 // TOKEN_TRUE
    { NULL, -1 },                 // TOKEN_WHILE
    { NULL, -1 },                 // TOKEN_XOR

    { NULL, -1 },                 // TOKEN_NAME
    { NULL, -1 },                 // TOKEN_NUMBER
    { NULL, -1 },                 // TOKEN_STRING

    { NULL, -1 },                 // TOKEN_LINE
    { NULL, -1 },                 // TOKEN_ERROR
    { NULL, -1 }                  // TOKEN_EOF
  };

  temp<Node> Parser::parseExpression(int precedence)
  {
    AllocScope scope;
    
    // Pratt operator precedence parser. See this for more:
    // http://journal.stuffwithstuff.com/2011/03/19/pratt-parsers-expression-parsing-made-easy/
    temp<Token> token = consume();
    PrefixParseFn prefix = prefixParsers_[token->type()];

    if (prefix == NULL)
    {
      // TODO(bob): Report error better.
      std::cout << "No prefix parser for " << token << "." << std::endl;
      return temp<Node>();
    }

    temp<Node> left = (this->*prefix)(token);

    while (precedence < infixParsers_[current().type()].precedence)
    {
      token = consume();

      InfixParseFn infix = infixParsers_[token->type()].fn;
      left = (this->*infix)(left, token);
    }

    return scope.close(left);
  }
  
  temp<Node> Parser::boolean(temp<Token> token)
  {
    return BoolNode::create(token->type() == TOKEN_TRUE);
  }
  
  temp<Node> Parser::ifThenElse(temp<Token> token)
  {
    AllocScope scope;
    
    temp<Node> condition = parseExpression();
    consume(TOKEN_THEN, "Expect 'then' after 'if' condition.");

    // TODO(bob): Block bodies.
    temp<Node> thenArm = parseExpression();
    // TODO(bob): Allow omitting 'else'.
    consume(TOKEN_ELSE, "Expect 'else' after 'then' arm.");
    temp<Node> elseArm = parseExpression();
    
    return scope.close(IfNode::create(condition, thenArm, elseArm));
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
    temp<Node> right = parseExpression(infixParsers_[token->type()].precedence);
    
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
    // TODO(bob): Is making a temp here right?
    return Memory::makeTemp(&(*read_.dequeue()));
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

