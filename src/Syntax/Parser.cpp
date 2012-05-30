#include <sstream>

#include "Lexer.h"
#include "Node.h"
#include "Parser.h"

namespace magpie
{
  enum Precedence {
    PRECEDENCE_ASSIGNMENT = 1, // =
    PRECEDENCE_RECORD     = 2, // ,
    PRECEDENCE_IS         = 3, // is
    PRECEDENCE_LOGICAL    = 4, // and or
    PRECEDENCE_NOT        = 5, // not
    PRECEDENCE_EQUALITY   = 6, // == !=
    PRECEDENCE_COMPARISON = 7, // < > <= >=
    PRECEDENCE_TERM       = 8, // + -
    PRECEDENCE_PRODUCT    = 9, // * / %
    PRECEDENCE_NEGATE     = 10, // -
    PRECEDENCE_CALL       = 11
  };

  Parser::Parselet Parser::expressions_[] = {
    // Punctuators.
    { &Parser::group,   NULL, -1 },                                 // TOKEN_LEFT_PAREN
    { NULL,             NULL, -1 },                                 // TOKEN_RIGHT_PAREN
    { NULL,             NULL, -1 },                                 // TOKEN_LEFT_BRACKET
    { NULL,             NULL, -1 },                                 // TOKEN_RIGHT_BRACKET
    { NULL,             NULL, -1 },                                 // TOKEN_LEFT_BRACE
    { NULL,             NULL, -1 },                                 // TOKEN_RIGHT_BRACE
    { NULL,             &Parser::infixRecord, PRECEDENCE_RECORD },  // TOKEN_COMMA
    { NULL,             NULL, -1 },                                 // TOKEN_EQ
    { NULL,             &Parser::binaryOp, PRECEDENCE_EQUALITY },   // TOKEN_EQEQ
    { NULL,             &Parser::binaryOp, PRECEDENCE_EQUALITY },   // TOKEN_NEQ
    { NULL,             &Parser::binaryOp, PRECEDENCE_COMPARISON }, // TOKEN_LT
    { NULL,             &Parser::binaryOp, PRECEDENCE_COMPARISON }, // TOKEN_GT
    { NULL,             &Parser::binaryOp, PRECEDENCE_COMPARISON }, // TOKEN_LTE
    { NULL,             &Parser::binaryOp, PRECEDENCE_COMPARISON }, // TOKEN_GTE
    { NULL,             &Parser::binaryOp, PRECEDENCE_TERM },       // TOKEN_PLUS
    { NULL,             &Parser::binaryOp, PRECEDENCE_TERM },       // TOKEN_MINUS
    { NULL,             &Parser::binaryOp, PRECEDENCE_PRODUCT },    // TOKEN_STAR
    { NULL,             &Parser::binaryOp, PRECEDENCE_PRODUCT },    // TOKEN_SLASH
    { NULL,             &Parser::binaryOp, PRECEDENCE_PRODUCT },    // TOKEN_PERCENT

    // Keywords.
    { NULL,             &Parser::and_, PRECEDENCE_LOGICAL },         // TOKEN_AND
    { NULL,             NULL, -1 },                                  // TOKEN_CASE
    { NULL,             NULL, -1 },                                  // TOKEN_CATCH
    { NULL,             NULL, -1 },                                  // TOKEN_DEF
    { NULL,             NULL, -1 },                                  // TOKEN_DO
    { NULL,             NULL, -1 },                                  // TOKEN_ELSE
    { NULL,             NULL, -1 },                                  // TOKEN_END
    { &Parser::boolean, NULL, -1 },                                  // TOKEN_FALSE
    { NULL,             NULL, -1 },                                  // TOKEN_FOR
    { NULL,             NULL, -1 },                                  // TOKEN_IF
    { NULL,             &Parser::is, PRECEDENCE_IS },                // TOKEN_IS
    { NULL,             NULL, -1 },                                  // TOKEN_MATCH
    { &Parser::not_,    NULL, -1 },                                  // TOKEN_NOT
    { &Parser::nothing, NULL, -1 },                                  // TOKEN_NOTHING
    { NULL,             &Parser::or_, PRECEDENCE_LOGICAL },          // TOKEN_OR
    { NULL,             NULL, -1 },                                  // TOKEN_RETURN
    { NULL,             NULL, -1 },                                  // TOKEN_THEN
    { &Parser::throw_,  NULL, -1 },                                  // TOKEN_THROW
    { &Parser::boolean, NULL, -1 },                                  // TOKEN_TRUE
    { NULL,             NULL, -1 },                                  // TOKEN_VAL
    { NULL,             NULL, -1 },                                  // TOKEN_VAR
    { NULL,             NULL, -1 },                                  // TOKEN_WHILE
    { NULL,             NULL, -1 },                                  // TOKEN_XOR

    { &Parser::record,  NULL, -1 },                                  // TOKEN_FIELD
    { &Parser::name,    &Parser::call, PRECEDENCE_CALL },            // TOKEN_NAME
    { &Parser::number,  NULL, -1 },                                  // TOKEN_NUMBER
    { &Parser::string,  NULL, -1 },                                  // TOKEN_STRING

    { NULL,             NULL, -1 },                                  // TOKEN_LINE
    { NULL,             NULL, -1 },                                  // TOKEN_ERROR
    { NULL,             NULL, -1 }                                   // TOKEN_EOF
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

    consume(TOKEN_EOF, "Expected end of file.");

    // TODO(bob): Should validate that we are at EOF here.
    return createSequence(exprs);
  }

  gc<Node> Parser::parseBlock(TokenType endToken)
  {
    TokenType dummy;
    return parseBlock(true, endToken, endToken, &dummy);
  }

  gc<Node> Parser::parseBlock(TokenType end1, TokenType end2,
                              TokenType* outEndToken)
  {
    return parseBlock(true, end1, end2, outEndToken);
  }

  gc<Node> Parser::parseBlock(bool allowCatch, TokenType end1, TokenType end2,
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
        if (lookAhead(TOKEN_CATCH)) break;
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

      gc<Node> block = createSequence(exprs);

      // Parse any catch clauses.
      if (allowCatch)
      {
        Array<MatchClause> catches;
        while (match(TOKEN_CATCH))
        {
          gc<Pattern> pattern = parsePattern();
          consume(TOKEN_THEN, "Expect 'then' after catch pattern.");
          gc<Node> body = parseBlock(false, end1, end2, outEndToken);
          catches.add(MatchClause(pattern, body));
        }

        if (catches.count() > 0)
        {
          block = new CatchNode(block->pos().spanTo(last()->pos()),
                                block, catches);
        }
      }

      return block;
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
    if (match(TOKEN_DEF))
    {
      SourcePos start = last()->pos();
      
      gc<Pattern> leftParam;
      if (match(TOKEN_LEFT_PAREN))
      {
        leftParam = parsePattern();
        consume(TOKEN_RIGHT_PAREN, "Expect ')' after pattern.");
      }
      
      gc<Token> name = consume(TOKEN_NAME,
                               "Expect a method name after 'def'.");
      
      gc<Pattern> rightParam;
      if (match(TOKEN_LEFT_PAREN))
      {
        if (match(TOKEN_RIGHT_PAREN))
        {
          // Allow () empty pattern to just mean "no parameter".
        }
        else
        {
          rightParam = parsePattern();
          consume(TOKEN_RIGHT_PAREN, "Expect ')' after pattern.");
        }
      }
      
      gc<Node> body = parseBlock();
      SourcePos span = start.spanTo(last()->pos());
      return new DefMethodNode(span, leftParam, name->text(), rightParam, body);
    }
    
    if (match(TOKEN_RETURN))
    {
      SourcePos start = last()->pos();
      
      // Parse the value if there is one.
      gc<Node> value;
      if (!lookAhead(TOKEN_LINE))
      {
        value = flowControl();
      }
      
      SourcePos span = start.spanTo(last()->pos());
      return new ReturnNode(span, value);
    }
    
    if (match(TOKEN_VAR) || match(TOKEN_VAL))
    {
      SourcePos start = last()->pos();
      
      // TODO(bob): Distinguish between var and val.
      bool isMutable = false;
      
      gc<Pattern> pattern = parsePattern();
      consume(TOKEN_EQ, "Expect '=' after variable declaration.");
      gc<Node> value = flowControl();
      
      SourcePos span = start.spanTo(last()->pos());
      return new VariableNode(span, isMutable, pattern, value);
    }
    
    return flowControl();
  }
  
  gc<Node> Parser::flowControl()
  {
    if (match(TOKEN_DO))
    {
      SourcePos start = last()->pos();
      gc<Node> body = parseBlock();
      SourcePos span = start.spanTo(last()->pos());
      return new DoNode(span, body);
    }

    if (match(TOKEN_IF))
    {
      SourcePos start = last()->pos();

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

      SourcePos span = start.spanTo(last()->pos());
      return new IfNode(span, condition, thenArm, elseArm);
    }

    if (match(TOKEN_MATCH))
    {
      SourcePos start = last()->pos();

      // Parse the value.
      gc<Node> value = parsePrecedence(PRECEDENCE_ASSIGNMENT);
      
      // Require a newline between the value and the first case.
      consume(TOKEN_LINE, "Expect a newline after a match's value expression.");
      
      // Parse the cases.
      Array<MatchClause> cases;
      while (match(TOKEN_CASE))
      {
        gc<Pattern> pattern = parsePattern();
        
        consume(TOKEN_THEN, "Expect 'then' after a case pattern.");
        
        TokenType endToken;
        gc<Node> body = parseBlock(TOKEN_ELSE, TOKEN_END, TOKEN_CASE,
                                   &endToken);
        
        // Allow newlines to separate single-line case and else cases.
        if ((endToken == TOKEN_EOF) &&
            (lookAhead(TOKEN_LINE, TOKEN_CASE) ||
             lookAhead(TOKEN_LINE, TOKEN_ELSE)))
        {
          consume(TOKEN_LINE, "");
        }
        
        cases.add(MatchClause(pattern, body));
      }
      
      consume(TOKEN_LINE,
              "Expect newline after last case in a match expression.");
      consume(TOKEN_END, "Expect 'end' after match expression.");
      
      SourcePos span = start.spanTo(last()->pos());
      return new MatchNode(span, value, cases);
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

  gc<Node> Parser::group(gc<Token> token)
  {
    gc<Node> node = flowControl();
    consume(TOKEN_RIGHT_PAREN, "Expect ')'.");
    return node;
  }

  gc<Node> Parser::name(gc<Token> token)
  {
    return call(gc<Node>(), token);
  }

  gc<Node> Parser::not_(gc<Token> token)
  {
    gc<Node> value = parsePrecedence(PRECEDENCE_CALL);
    return new NotNode(token->pos().spanTo(value->pos()), value);
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

  gc<Node> Parser::record(gc<Token> token)
  {
    Array<Field> fields;

    gc<Node> value = parsePrecedence(PRECEDENCE_LOGICAL);
    fields.add(Field(token->text(), value));

    int i = 1;
    while (match(TOKEN_COMMA))
    {
      gc<String> name;
      if (match(TOKEN_FIELD))
      {
        // A named field.
        name = last()->text();
      }
      else
      {
        // Infer the name from its position.
        name = String::format("%d", i);
      }

      value = parsePrecedence(PRECEDENCE_LOGICAL);
      fields.add(Field(name, value));

      i++;
    }

    return new RecordNode(token->pos().spanTo(last()->pos()), fields);
  }

  gc<Node> Parser::string(gc<Token> token)
  {
    return new StringNode(token->pos(), token->text());
  }

  gc<Node> Parser::throw_(gc<Token> token)
  {
    gc<Node> value = parsePrecedence(PRECEDENCE_LOGICAL);
    return new ThrowNode(token->pos().spanTo(last()->pos()), value);
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

  gc<Node> Parser::call(gc<Node> left, gc<Token> token)
  {
    // See if we have an argument on the right.
    bool hasRightArg = false;
    gc<Node> right;
    if (match(TOKEN_LEFT_PAREN))
    {
      hasRightArg = true;
      if (match(TOKEN_RIGHT_PAREN))
      {
        // Allow () to distinguish a call from a name, but don't provide an
        // argument.
      }
      else
      {
        // TODO(bob): Is this right? Do we want to allow variable declarations
        // here?
        right = statementLike();
        consume(TOKEN_RIGHT_PAREN, "Expect ')' after call argument.");
      }
    }

    if (left.isNull() && !hasRightArg)
    {
      // Just a bare name.
      return new NameNode(token->pos(), token->text());
    }

    // TODO(bob): Better position.
    return new CallNode(token->pos(), left, token->text(), right);
  }

  gc<Node> Parser::infixRecord(gc<Node> left, gc<Token> token)
  {
    Array<Field> fields;

    // First field is positional.
    gc<String> name = String::create("0");
    fields.add(Field(name, left));

    int i = 1;
    do
    {
      gc<String> name;
      if (match(TOKEN_FIELD))
      {
        // A named field.
        name = last()->text();
      }
      else
      {
        // Infer the name from its position.
        name = String::format("%d", i);
      }

      gc<Node> value = parsePrecedence(PRECEDENCE_LOGICAL);
      fields.add(Field(name, value));

      i++;
    }
    while (match(TOKEN_COMMA));

    return new RecordNode(left->pos().spanTo(last()->pos()), fields);
  }
  
  gc<Node> Parser::is(gc<Node> left, gc<Token> token)
  {
    gc<Node> type = parsePrecedence(PRECEDENCE_IS + 1);
    return new IsNode(token->pos(), left, type);
  }
  
  gc<Node> Parser::or_(gc<Node> left, gc<Token> token)
  {
    gc<Node> right = parsePrecedence(expressions_[token->type()].precedence);
    return new OrNode(token->pos(), left, right);
  }

  // Pattern parsers ----------------------------------------------------------

  gc<Pattern> Parser::parsePattern()
  {
    return recordPattern();
  }

  gc<Pattern> Parser::recordPattern()
  {
    SourcePos pos = current().pos();
    Array<PatternField> fields;

    do
    {
      gc<String> name;
      if (match(TOKEN_FIELD))
      {
        name = last()->text();
      }

      gc<Pattern> value = variablePattern();
      
      if (value.isNull())
      {
        reporter_.error(current().pos(), "Expect pattern.");
      }
      
      fields.add(PatternField(name, value));
    }
    while (match(TOKEN_COMMA));

    // If we just have a single unnamed field, it's not a record, it's just
    // that pattern.
    if (fields.count() == 1 && fields[0].name.isNull())
    {
      return fields[0].value;
    }

    // Fill in the positional names.
    for (int i = 0; i < fields.count(); i++)
    {
      if (fields[i].name.isNull())
      {
        fields[i].name = String::format("%d", i);
      }
    }

    return new RecordPattern(pos.spanTo(last()->pos()), fields);
  }

  gc<Pattern> Parser::variablePattern()
  {
    if (match(TOKEN_NAME))
    {
      gc<String> name = last()->text();
      gc<Pattern> inner = primaryPattern();
      return new VariablePattern(last()->pos(), name, inner);
    }
    else
    {
      return primaryPattern();
    }
  }
  
  gc<Pattern> Parser::primaryPattern()
  {
    if (match(TOKEN_TRUE) || match(TOKEN_FALSE))
    {
      gc<Node> value = boolean(last());
      return new ValuePattern(last()->pos(), value);
    }
    
    if (match(TOKEN_EQEQ))
    {
      SourcePos start = last()->pos();
      gc<Node> value = parsePrecedence(PRECEDENCE_COMPARISON);
      return new ValuePattern(start.spanTo(last()->pos()), value);
    }
    
    if (match(TOKEN_IS))
    {
      SourcePos start = last()->pos();
      gc<Node> type = parsePrecedence(PRECEDENCE_COMPARISON);
      return new TypePattern(start.spanTo(last()->pos()), type);
    }
    
    if (match(TOKEN_NOTHING))
    {
      gc<Node> value = nothing(last());
      return new ValuePattern(last()->pos(), value);
    }
    
    if (match(TOKEN_NUMBER))
    {
      gc<Node> value = number(last());
      return new ValuePattern(last()->pos(), value);
    }
    
    if (match(TOKEN_STRING))
    {
      gc<Node> value = string(last());
      return new ValuePattern(last()->pos(), value);
    }
    
    if (match(TOKEN_LEFT_PAREN))
    {
      gc<Pattern> pattern = parsePattern();
      consume(TOKEN_RIGHT_PAREN, "Expect ')' after pattern.");
      return pattern;
    }

    return gc<Pattern>();
  }

  // Helpers and base methods -------------------------------------------------

  gc<Node> Parser::createSequence(const Array<gc<Node> >& exprs)
  {
    // If the sequence is empty, just default it to nothing.
    if (exprs.count() == 0) return new NothingNode(last()->pos());

    // If there is just one expression in the sequence, don't wrap it.
    if (exprs.count() == 1) return exprs[0];

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
    last_ = read_.dequeue();
    return last_;
  }

  gc<Token> Parser::consume(TokenType expected, const char* errorMessage)
  {
    if (lookAhead(expected)) return consume();
    reporter_.error(current().pos(), errorMessage);

    // Just so that we can keep going and try to find other errors, consume the
    // failed token and proceed.
    return consume();
  }

  void Parser::fillLookAhead(int count)
  {
    while (read_.count() < count)
    {
      gc<Token> token = lexer_.readToken();
      if (token->is(TOKEN_ERROR))
      {
        reporter_.error(token->pos(), token->text()->cString());
      }
      else
      {
        read_.enqueue(token);
      }
    }
  }
}

