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
    { NULL,                 &Parser::binaryOp, 4 },     // TOKEN_LESS_THAN

    // Keywords.
    { NULL,                 &Parser::binaryOp, 3 },     // TOKEN_AND
    { NULL,                 NULL, -1 },                 // TOKEN_CASE
    { NULL,                 NULL, -1 },                 // TOKEN_DEF
    { NULL,                 NULL, -1 },                 // TOKEN_DO
    { NULL,                 NULL, -1 },                 // TOKEN_ELSE
    { NULL,                 NULL, -1 },                 // TOKEN_END
    { &Parser::boolean,     NULL, -1 },                 // TOKEN_FALSE
    { NULL,                 NULL, -1 },                 // TOKEN_FOR
    { NULL,                 NULL, -1 },                 // TOKEN_IF
    { NULL,                 NULL, -1 },                 // TOKEN_IS
    { NULL,                 NULL, -1 },                 // TOKEN_MATCH
    { NULL,                 NULL, -1 },                 // TOKEN_NOT
    { NULL,                 &Parser::binaryOp, 3 },     // TOKEN_OR
    { NULL,                 NULL, -1 },                 // TOKEN_RETURN
    { NULL,                 NULL, -1 },                 // TOKEN_THEN
    { &Parser::boolean,     NULL, -1 },                 // TOKEN_TRUE
    { NULL,                 NULL, -1 },                 // TOKEN_VAL
    { NULL,                 NULL, -1 },                 // TOKEN_VAR
    { NULL,                 NULL, -1 },                 // TOKEN_WHILE
    { NULL,                 NULL, -1 },                 // TOKEN_XOR

    { &Parser::name,        NULL, -1 },                 // TOKEN_NAME
    { &Parser::number,      NULL, -1 },                 // TOKEN_NUMBER
    { &Parser::string,      NULL, -1 },                 // TOKEN_STRING

    { NULL,                 NULL, -1 },                 // TOKEN_LINE
    { NULL,                 NULL, -1 },                 // TOKEN_ERROR
    { NULL,                 NULL, -1 }                  // TOKEN_EOF
  };

  temp<ModuleAst> Parser::parseModule()
  {
    // TODO(bob): Right now this just returns a sequence. Should probably have
    // a different node type for the top level.
    Array<gc<MethodAst> > methods;
    
    do
    {
      // Method definition.
      consume(TOKEN_DEF, "The top level of a module contains only method definitions.");
      temp<Token> name = consume(TOKEN_NAME,
                                 "Expect a method name after 'def'.");
      
      // TODO(bob): Parse real pattern(s).
      temp<Pattern> pattern;
      consume(TOKEN_LEFT_PAREN, "Temp.");
      if (lookAhead(TOKEN_NAME))
      {
        pattern = parsePattern();
      }
      consume(TOKEN_RIGHT_PAREN, "Temp.");
      
      temp<Node> body = parseBlock();
      
      methods.add(MethodAst::create(name->text(), pattern, body));
    }
    while (match(TOKEN_LINE));
    
    return ModuleAst::create(methods);
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
      
      SourcePos span = exprs[0]->pos().spanTo(current().pos());
      
      // TODO(bob): Don't wrap in a sequence if there's just one.
      return SequenceNode::create(span, exprs);
    }
    else
    {
      return statementLike();
    }
  }

  temp<Node> Parser::statementLike()
  {
    AllocScope scope;
    
    if (lookAhead(TOKEN_IF))
    {
      SourcePos start = consume()->pos();
      
      temp<Node> condition = parsePrecedence();
      consume(TOKEN_THEN, "Expect 'then' after 'if' condition.");
      
      // TODO(bob): Block bodies.
      temp<Node> thenArm = parsePrecedence();
      // TODO(bob): Allow omitting 'else'.
      consume(TOKEN_ELSE, "Expect 'else' after 'then' arm.");
      temp<Node> elseArm = parsePrecedence();
      
      SourcePos span = start.spanTo(current().pos());
      return scope.close(IfNode::create(span, condition, thenArm, elseArm));
    }
    
    if (lookAhead(TOKEN_VAR) || lookAhead(TOKEN_VAL))
    {
      SourcePos start = consume()->pos();
      
      // TODO(bob): Distinguish between var and val.
      bool isMutable = false;
      
      temp<Pattern> pattern = parsePattern();
      consume(TOKEN_EQUALS, "Expect '=' after variable declaration.");
      // TODO(bob): What precedence?
      temp<Node> value = parsePrecedence();
      
      SourcePos span = start.spanTo(current().pos());
      return scope.close(VariableNode::create(span, isMutable, pattern, value));
    }
    
    return parsePrecedence();
  }
  
  temp<Node> Parser::parsePrecedence(int precedence)
  {
    AllocScope scope;
    temp<Token> token = consume();
    PrefixParseFn prefix = expressions_[token->type()].prefix;
    
    if (prefix == NULL)
    {
      reporter_.error(token->pos(), "Unexpected token '%s'.",
                      token->toString()->cString());
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
    return BoolNode::create(token->pos(), token->type() == TOKEN_TRUE);
  }
  
  temp<Node> Parser::name(temp<Token> token)
  {
    // See if it's a method call like foo(arg).
    if (match(TOKEN_LEFT_PAREN))
    {
      temp<Node> arg = parsePrecedence();
      consume(TOKEN_RIGHT_PAREN, "Expect ')' after call argument.");

      SourcePos span = token->pos().spanTo(current().pos());
      return CallNode::create(span, gc<Node>(), token->text(), arg);
    }
    else
    {
      // Just a bare name.
      return NameNode::create(token->pos(), token->text());
    }
  }
  
  temp<Node> Parser::number(temp<Token> token)
  {
    double number = atof(token->text()->cString());
    return NumberNode::create(token->pos(), number);
  }

  temp<Node> Parser::string(temp<Token> token)
  {
    return StringNode::create(token->pos(), token->text());
  }
  
  temp<Node> Parser::binaryOp(temp<Node> left, temp<Token> token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    temp<Node> right = parsePrecedence(expressions_[token->type()].precedence);
    
    return BinaryOpNode::create(token->pos(), left, token->type(), right);
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
      reporter_.error(current().pos(), "Expected pattern.");
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
    if (!lookAhead(expected)) reporter_.error(current().pos(), errorMessage);
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

    reporter_.error(current().pos(), errorMessage);
    return temp<Token>();
  }

  void Parser::fillLookAhead(int count)
  {
    while (read_.count() < count) read_.enqueue(lexer_.readToken());
  }
}

