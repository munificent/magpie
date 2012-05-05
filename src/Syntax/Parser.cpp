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
    { NULL,                 &Parser::and_, 3 },         // TOKEN_AND
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
    { &Parser::nothing,     NULL, -1 },                 // TOKEN_NOTHING
    { NULL,                 &Parser::or_, 3 },          // TOKEN_OR
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

  gc<Node> Parser::parseModule()
  {
    Array<gc<Node> > exprs;
    
    do
    {
      if (lookAhead(TOKEN_EOF)) break;
      exprs.add(statementLike());
    }
    while (match(TOKEN_LINE));
    
    // TODO(bob): Should validate that we are at EOF here.
    return createSequence(exprs);
  }
  
  gc<Node> Parser::parseBlock(TokenType endToken)
  {
    TokenType dummy;
    return parseBlock(endToken, endToken, &dummy);
  }

  gc<Node> Parser::parseBlock(TokenType end1, TokenType end2,
                              TokenType* outEndToken)
  {
    // If we have a newline, then it's an actual block, otherwise it's a
    // single expression.
    if (match(TOKEN_LINE))
    {
      Array<gc<Node> > exprs;
      
      do
      {
        if (lookAhead(end1)) break;
        if (lookAhead(end2)) break;
        exprs.add(statementLike());
      }
      while (match(TOKEN_LINE));

      // Return which kind of token we ended the block with, for callers that
      // care.
      *outEndToken = current().type();

      // If the block ends with 'end', then we want to consume that token,
      // otherwise we want to leave it unconsumed to be consistent with the
      // single-expression block case.
      if (current().is(TOKEN_END)) consume();
      
      return createSequence(exprs);
    }
    else
    {
      // Not a block, so no block end token.
      *outEndToken = TOKEN_EOF;
      return statementLike();
    }
  }

  gc<Node> Parser::statementLike()
  {
    if (lookAhead(TOKEN_DEF))
    {
      SourcePos start = consume()->pos();
      gc<Token> name = consume(TOKEN_NAME,
                               "Expect a method name after 'def'.");

      // TODO(bob): Parse real pattern(s). Handle prefix, infix, postfix
      // methods.
      gc<Pattern> pattern = NULL;
      consume(TOKEN_LEFT_PAREN, "Temp.");
      if (lookAhead(TOKEN_NAME))
      {
        pattern = parsePattern();
      }
      consume(TOKEN_RIGHT_PAREN, "Temp.");
      
      gc<Node> body = parseBlock();
      SourcePos span = start.spanTo(current().pos());
      return new DefMethodNode(span, name->text(), pattern, body);
    }
    
    if (lookAhead(TOKEN_DO))
    {
      SourcePos start = consume()->pos();
      gc<Node> body = parseBlock();
      SourcePos span = start.spanTo(current().pos());
      return new DoNode(span, body);
    }
    
    if (lookAhead(TOKEN_IF))
    {
      SourcePos start = consume()->pos();
      
      gc<Node> condition = parseBlock(TOKEN_THEN);
      consume(TOKEN_THEN, "Expect 'then' after 'if' condition.");
      
      TokenType endToken;
      gc<Node> thenArm = parseBlock(TOKEN_ELSE, TOKEN_END, &endToken);
      gc<Node> elseArm;
      
      // Don't look for an else arm if the then arm was a block that
      // specifically ended with 'end'.
      if ((endToken != TOKEN_END) && match(TOKEN_ELSE))
      {
        elseArm = parseBlock();
      }
      
      SourcePos span = start.spanTo(current().pos());
      return new IfNode(span, condition, thenArm, elseArm);
    }
    
    if (lookAhead(TOKEN_VAR) || lookAhead(TOKEN_VAL))
    {
      SourcePos start = consume()->pos();
      
      // TODO(bob): Distinguish between var and val.
      bool isMutable = false;
      
      gc<Pattern> pattern = parsePattern();
      consume(TOKEN_EQUALS, "Expect '=' after variable declaration.");
      // TODO(bob): What precedence?
      gc<Node> value = parsePrecedence();
      
      SourcePos span = start.spanTo(current().pos());
      return new VariableNode(span, isMutable, pattern, value);
    }
    
    return parsePrecedence();
  }
  
  gc<Node> Parser::parsePrecedence(int precedence)
  {
    gc<Token> token = consume();
    PrefixParseFn prefix = expressions_[token->type()].prefix;
    
    if (prefix == NULL)
    {
      gc<String> tokenText = token->toString();
      reporter_.error(token->pos(), "Unexpected token '%s'.",
                      tokenText->cString());
      return NULL;
    }
    
    gc<Node> left = (this->*prefix)(token);
    
    while (precedence < expressions_[current().type()].precedence)
    {
      token = consume();
      InfixParseFn infix = expressions_[token->type()].infix;
      left = (this->*infix)(left, token);
    }
    
    return left;
  }
  
  // Prefix parsers -----------------------------------------------------------

  gc<Node> Parser::boolean(gc<Token> token)
  {
    return new BoolNode(token->pos(), token->type() == TOKEN_TRUE);
  }
  
  gc<Node> Parser::name(gc<Token> token)
  {
    // See if it's a method call like foo(arg).
    if (match(TOKEN_LEFT_PAREN))
    {
      gc<Node> arg;
      if (match(TOKEN_RIGHT_PAREN))
      {
        // Implicit "nothing" argument.
        arg = new NothingNode(current().pos());
      }
      else
      {
        // TODO(bob): Is this right? Do we want to allow variable declarations
        // here?
        arg = statementLike();
        consume(TOKEN_RIGHT_PAREN, "Expect ')' after call argument.");
      }

      SourcePos span = token->pos().spanTo(current().pos());
      return new CallNode(span, NULL, token->text(), arg);
    }
    else
    {
      // Just a bare name.
      return new NameNode(token->pos(), token->text());
    }
  }
  
  gc<Node> Parser::nothing(gc<Token> token)
  {
    return new NothingNode(token->pos());
  }
  
  gc<Node> Parser::number(gc<Token> token)
  {
    double number = atof(token->text()->cString());
    return new NumberNode(token->pos(), number);
  }

  gc<Node> Parser::string(gc<Token> token)
  {
    return new StringNode(token->pos(), token->text());
  }
  
  // Infix parsers ------------------------------------------------------------
  
  gc<Node> Parser::and_(gc<Node> left, gc<Token> token)
  {
    gc<Node> right = parsePrecedence(expressions_[token->type()].precedence);
    return new AndNode(token->pos(), left, right);
  }
  
  gc<Node> Parser::binaryOp(gc<Node> left, gc<Token> token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    gc<Node> right = parsePrecedence(expressions_[token->type()].precedence);
    
    return new BinaryOpNode(token->pos(), left, token->type(), right);
  }
  
  gc<Node> Parser::or_(gc<Node> left, gc<Token> token)
  {
    gc<Node> right = parsePrecedence(expressions_[token->type()].precedence);
    return new OrNode(token->pos(), left, right);
  }
  
  gc<Pattern> Parser::parsePattern()
  {
    return variablePattern();
  }
  
  gc<Pattern> Parser::variablePattern()
  {
    if (lookAhead(TOKEN_NAME))
    {
      gc<Token> token = consume();
      return new VariablePattern(token->pos(), token->text());
    }
    else
    {
      reporter_.error(current().pos(), "Expected pattern.");
      return gc<Pattern>();
    }
  }
  
  gc<Node> Parser::createSequence(const Array<gc<Node> >& exprs)
  {
    // If there is just one expression in the sequence, don't wrap it.
    if (exprs.count() == 1) return exprs[0];
    
    // TODO(bob): Using current() here and elsewhere is wrong. That's one
    // token past the span.
    SourcePos span = exprs[0]->pos().spanTo(exprs[-1]->pos());
    return new SequenceNode(span, exprs);
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

  gc<Token> Parser::consume()
  {
    fillLookAhead(1);
    return read_.dequeue();
  }

  gc<Token> Parser::consume(TokenType expected, const char* errorMessage)
  {
    if (lookAhead(expected)) return consume();
    reporter_.error(current().pos(), errorMessage);
    return NULL;
  }

  void Parser::fillLookAhead(int count)
  {
    while (read_.count() < count) read_.enqueue(lexer_.readToken());
  }
}

