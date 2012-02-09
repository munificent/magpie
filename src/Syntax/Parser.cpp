#include <sstream>

#include "Lexer.h"
#include "Node.h"
#include "Parser.h"

namespace magpie
{
  // TODO(bob): Figure out full precedence table.
  Parser::Parselet Parser::expressions_[] = {
    // Punctuators.
    { NULL,                 NULL, -1 },                 // TOKEN_LEFT_PAREN
    { NULL,                 NULL, -1 },                 // TOKEN_RIGHT_PAREN
    { NULL,                 NULL, -1 },                 // TOKEN_LEFT_BRACKET
    { NULL,                 NULL, -1 },                 // TOKEN_RIGHT_BRACKET
    { NULL,                 NULL, -1 },                 // TOKEN_LEFT_BRACE
    { NULL,                 NULL, -1 },                 // TOKEN_RIGHT_BRACE
    { NULL,                 NULL, -1 },                 // TOKEN_EQUALS
    { NULL,                 &Parser::binaryOp, 7 },     // TOKEN_PLUS
    { NULL,                 &Parser::binaryOp, 7 },     // TOKEN_MINUS
    { NULL,                 &Parser::binaryOp, 8 },     // TOKEN_STAR
    { NULL,                 &Parser::binaryOp, 8 },     // TOKEN_SLASH
    { NULL,                 &Parser::binaryOp, 8 },     // TOKEN_PERCENT

    // Keywords.
    { NULL,                 &Parser::binaryOp, 3 },     // TOKEN_AND
    { NULL,                 NULL, -1 },                 // TOKEN_CASE
    { NULL,                 NULL, -1 },                 // TOKEN_DEF
    { NULL,                 NULL, -1 },                 // TOKEN_DO
    { NULL,                 NULL, -1 },                 // TOKEN_ELSE
    { NULL,                 NULL, -1 },                 // TOKEN_END
    { &Parser::boolean,     NULL, -1 },                 // TOKEN_FALSE
    { NULL,                 NULL, -1 },                 // TOKEN_FOR
    { &Parser::ifThenElse,  NULL, -1 },                 // TOKEN_IF
    { NULL,                 NULL, -1 },                 // TOKEN_IS
    { NULL,                 NULL, -1 },                 // TOKEN_MATCH
    { NULL,                 NULL, -1 },                 // TOKEN_NOT
    { NULL,                 &Parser::binaryOp, 3 },     // TOKEN_OR
    { NULL,                 NULL, -1 },                 // TOKEN_RETURN
    { NULL,                 NULL, -1 },                 // TOKEN_THEN
    { &Parser::boolean,     NULL, -1 },                 // TOKEN_TRUE
    { &Parser::variable,    NULL, -1 },                 // TOKEN_VAL
    { &Parser::variable,    NULL, -1 },                 // TOKEN_VAR
    { NULL,                 NULL, -1 },                 // TOKEN_WHILE
    { NULL,                 NULL, -1 },                 // TOKEN_XOR

    { &Parser::name,        NULL, -1 },                 // TOKEN_NAME
    { &Parser::number,      NULL, -1 },                 // TOKEN_NUMBER
    { NULL,                 NULL, -1 },                 // TOKEN_STRING

    { NULL,                 NULL, -1 },                 // TOKEN_LINE
    { NULL,                 NULL, -1 },                 // TOKEN_ERROR
    { NULL,                 NULL, -1 }                  // TOKEN_EOF
  };

  temp<Node> Parser::parseProgram()
  {
    // TODO(bob): Right now this just returns a sequence. Should probably have
    // a different node type for the top level.
    Array<gc<Node> > exprs;
    
    do
    {
      exprs.add(statementLike());
    }
    while (match(TOKEN_LINE));
    
    return SequenceNode::create(exprs);
  }
  
  temp<Node> Parser::parseBlock()
  {
    // If we have a newline, then it's an actual block, otherwise it's a
    // single expression.
    if (match(TOKEN_LINE))
    {
      Array<gc<Node> > exprs;
      
      do
      {
        if (match(TOKEN_END)) break;
        exprs.add(statementLike());
      }
      while (match(TOKEN_LINE));
      
      // TODO(bob): Don't wrap in a sequence if there's just one.
      return SequenceNode::create(exprs);
    }
    else
    {
      return statementLike();
    }
  }

  temp<Node> Parser::statementLike()
  {
    // TODO(rnystrom): Move if, var and val here.
    if (match(TOKEN_DEF))
    {
      // Method definition.
      temp<Token> name = consume(TOKEN_NAME,
          "Expect a method name after 'def'.");
      
      // TODO(bob): Parse parameter pattern(s).
      consume(TOKEN_LEFT_PAREN, "Temp.");
      consume(TOKEN_RIGHT_PAREN, "Temp.");
      
      temp<Node> body = parseBlock();
      
      return MethodNode::create(name->text(), body);
    }
    else
    {
      // Otherwise it must be a normal expression.
      return parsePrecedence();
    }
  }
  
  temp<Node> Parser::parsePrecedence(int precedence)
  {
    AllocScope scope;
    temp<Token> token = consume();
    PrefixParseFn prefix = expressions_[token->type()].prefix;
    
    if (prefix == NULL)
    {
      // TODO(bob): Report error better.
      std::cout << "No prefix parser for " << token << "." << std::endl;
      return temp<Node>();
    }
    
    temp<Node> left = (this->*prefix)(token);
    
    while (precedence < expressions_[current().type()].precedence)
    {
      token = consume();
      
      InfixParseFn infix = expressions_[token->type()].infix;
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
    
    temp<Node> condition = parsePrecedence();
    consume(TOKEN_THEN, "Expect 'then' after 'if' condition.");

    // TODO(bob): Block bodies.
    temp<Node> thenArm = parsePrecedence();
    // TODO(bob): Allow omitting 'else'.
    consume(TOKEN_ELSE, "Expect 'else' after 'then' arm.");
    temp<Node> elseArm = parsePrecedence();
    
    return scope.close(IfNode::create(condition, thenArm, elseArm));
  }
  
  temp<Node> Parser::name(temp<Token> token)
  {
    return NameNode::create(token->text());
  }
  
  temp<Node> Parser::number(temp<Token> token)
  {
    double number = atof(token->text()->cString());
    return NumberNode::create(number);
  }
  
  temp<Node> Parser::variable(temp<Token> token)
  {
    bool isMutable = token->type() == TOKEN_VAR;

    temp<Pattern> pattern = parsePattern();
    consume(TOKEN_EQUALS, "Expect '=' after variable declaration.");
    // TODO(bob): What precedence?
    temp<Node> value = parsePrecedence();
    
    return VariableNode::create(isMutable, pattern, value);
  }
  
  temp<Node> Parser::binaryOp(temp<Node> left, temp<Token> token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    temp<Node> right = parsePrecedence(expressions_[token->type()].precedence);
    
    return BinaryOpNode::create(left, token->type(), right);
  }
  
  temp<Pattern> Parser::parsePattern()
  {
    return variablePattern();
  }
  
  temp<Pattern> Parser::variablePattern()
  {
    if (lookAhead(TOKEN_NAME))
    {
      return VariablePattern::create(consume()->text());
    }
    else
    {
      error("Expected pattern.");
      return temp<Pattern>();
    }
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

