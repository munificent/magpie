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

  ModuleAst* Parser::parseModule()
  {
    // TODO(bob): Right now this just returns a sequence. Should probably have
    // a different node type for the top level.
    Array<MethodAst*> methods;
    
    do
    {
      // Method definition.
      consume(TOKEN_DEF, "The top level of a module contains only method definitions.");
      Token* name = consume(TOKEN_NAME,
                            "Expect a method name after 'def'.");
      
      // TODO(bob): Parse real pattern(s).
      Pattern* pattern = NULL;
      consume(TOKEN_LEFT_PAREN, "Temp.");
      if (lookAhead(TOKEN_NAME))
      {
        pattern = parsePattern();
      }
      consume(TOKEN_RIGHT_PAREN, "Temp.");
      
      Node* body = parseBlock();
      
      methods.add(new MethodAst(name->text(), pattern, body));
    }
    while (match(TOKEN_LINE));
    
    return new ModuleAst(methods);
  }
  
  Node* Parser::parseBlock()
  {
    // If we have a newline, then it's an actual block, otherwise it's a
    // single expression.
    if (match(TOKEN_LINE))
    {
      Array<Node*> exprs;
      
      do
      {
        if (match(TOKEN_END)) break;
        exprs.add(statementLike());
      }
      while (match(TOKEN_LINE));
      
      // If there is just one expression in the sequence, don't wrap it.
      if (exprs.count() == 1) return exprs[0];
      
      SourcePos span = exprs[0]->pos().spanTo(current().pos());
      return new SequenceNode(span, exprs);
    }
    else
    {
      return statementLike();
    }
  }

  Node* Parser::statementLike()
  {
    if (lookAhead(TOKEN_IF))
    {
      SourcePos start = consume()->pos();
      
      Node* condition = parsePrecedence();
      consume(TOKEN_THEN, "Expect 'then' after 'if' condition.");
      
      // TODO(bob): Block bodies.
      Node* thenArm = parsePrecedence();
      // TODO(bob): Allow omitting 'else'.
      consume(TOKEN_ELSE, "Expect 'else' after 'then' arm.");
      Node* elseArm = parsePrecedence();
      
      SourcePos span = start.spanTo(current().pos());
      return new IfNode(span, condition, thenArm, elseArm);
    }
    
    if (lookAhead(TOKEN_VAR) || lookAhead(TOKEN_VAL))
    {
      SourcePos start = consume()->pos();
      
      // TODO(bob): Distinguish between var and val.
      bool isMutable = false;
      
      Pattern* pattern = parsePattern();
      consume(TOKEN_EQUALS, "Expect '=' after variable declaration.");
      // TODO(bob): What precedence?
      Node* value = parsePrecedence();
      
      SourcePos span = start.spanTo(current().pos());
      return new VariableNode(span, isMutable, pattern, value);
    }
    
    return parsePrecedence();
  }
  
  Node* Parser::parsePrecedence(int precedence)
  {
    Token* token = consume();
    PrefixParseFn prefix = expressions_[token->type()].prefix;
    
    if (prefix == NULL)
    {
      reporter_.error(token->pos(), "Unexpected token '%s'.",
                      token->text()->cString());
      return NULL;
    }
    
    Node* left = (this->*prefix)(token);
    
    while (precedence < expressions_[current().type()].precedence)
    {
      token = consume();
      
      InfixParseFn infix = expressions_[token->type()].infix;
      left = (this->*infix)(left, token);
    }
    
    return left;
  }
  
  Node* Parser::boolean(Token* token)
  {
    return new BoolNode(token->pos(), token->type() == TOKEN_TRUE);
  }
  
  Node* Parser::name(Token* token)
  {
    // See if it's a method call like foo(arg).
    if (match(TOKEN_LEFT_PAREN))
    {
      Node* arg = parsePrecedence();
      consume(TOKEN_RIGHT_PAREN, "Expect ')' after call argument.");

      SourcePos span = token->pos().spanTo(current().pos());
      return new CallNode(span, NULL, token->text(), arg);
    }
    else
    {
      // Just a bare name.
      return new NameNode(token->pos(), token->text());
    }
  }
  
  Node* Parser::number(Token* token)
  {
    double number = atof(token->text()->cString());
    return new NumberNode(token->pos(), number);
  }

  Node* Parser::string(Token* token)
  {
    return new StringNode(token->pos(), token->text());
  }
  
  Node* Parser::binaryOp(Node* left, Token* token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    Node* right = parsePrecedence(expressions_[token->type()].precedence);
    
    return new BinaryOpNode(token->pos(), left, token->type(), right);
  }
  
  Pattern* Parser::parsePattern()
  {
    return variablePattern();
  }
  
  Pattern* Parser::variablePattern()
  {
    if (lookAhead(TOKEN_NAME))
    {
      return new VariablePattern(consume()->text());
    }
    else
    {
      reporter_.error(current().pos(), "Expected pattern.");
      return NULL;
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

  Token* Parser::consume()
  {
    fillLookAhead(1);
    // TODO(bob): Memory leak! Tokens aren't being deleted after being consumed.
    // Either make sure they get deleted, or switch to a zone allocator.
    return read_.dequeue();
  }

  Token* Parser::consume(TokenType expected, const char* errorMessage)
  {
    if (lookAhead(expected)) return consume();

    std::cout << current() << std::endl;
    reporter_.error(current().pos(), errorMessage);
    return NULL;
  }

  void Parser::fillLookAhead(int count)
  {
    while (read_.count() < count) read_.enqueue(lexer_.readToken());
  }
}

