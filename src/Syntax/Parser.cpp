#include <sstream>

#include "Ast.h"
#include "Lexer.h"
#include "Parser.h"

namespace magpie
{
  enum Precedence {
    PRECEDENCE_ASSIGNMENT = 1, // =
    PRECEDENCE_RECORD     = 2, // ,
    PRECEDENCE_LOGICAL    = 3, // and or
    PRECEDENCE_NOT        = 4, // not
    PRECEDENCE_IS         = 5, // is
    PRECEDENCE_EQUALITY   = 6, // == !=
    PRECEDENCE_COMPARISON = 7, // < > <= >=
    PRECEDENCE_RANGE      = 8, // .. ...
    PRECEDENCE_TERM       = 9, // + -
    PRECEDENCE_PRODUCT    = 10, // * / %
    PRECEDENCE_NEGATE     = 11, // -
    PRECEDENCE_CALL       = 12
  };

  Parser::Parselet Parser::expressions_[] = {
    // Punctuators.
    { &Parser::group,   NULL, -1 },                                   // TOKEN_LEFT_PAREN
    { NULL,             NULL, -1 },                                   // TOKEN_RIGHT_PAREN
    { &Parser::list,    &Parser::subscript, PRECEDENCE_CALL },        // TOKEN_LEFT_BRACKET
    { NULL,             NULL, -1 },                                   // TOKEN_RIGHT_BRACKET
    { NULL,             NULL, -1 },                                   // TOKEN_LEFT_BRACE
    { NULL,             NULL, -1 },                                   // TOKEN_RIGHT_BRACE
    { NULL,             &Parser::infixRecord, PRECEDENCE_RECORD },    // TOKEN_COMMA
    { NULL,             NULL, -1 },                                   // TOKEN_DOT
    { NULL,             &Parser::infixCall, PRECEDENCE_RANGE },       // TOKEN_DOTDOT
    { NULL,             &Parser::infixCall, PRECEDENCE_RANGE },       // TOKEN_DOTDOTDOT
    { NULL,             &Parser::assignment, PRECEDENCE_ASSIGNMENT }, // TOKEN_EQ
    { NULL,             &Parser::binaryOp, PRECEDENCE_EQUALITY },     // TOKEN_EQEQ
    { NULL,             &Parser::binaryOp, PRECEDENCE_EQUALITY },     // TOKEN_NEQ
    { NULL,             &Parser::infixCall, PRECEDENCE_COMPARISON },  // TOKEN_COMPARISON
    { NULL,             &Parser::infixCall, PRECEDENCE_TERM },        // TOKEN_TERM_OP
    { NULL,             &Parser::infixCall, PRECEDENCE_PRODUCT },     // TOKEN_PRODUCT_OP

    // Keywords.
    { NULL,             &Parser::and_, PRECEDENCE_LOGICAL },           // TOKEN_AND
    { NULL,             NULL, -1 },                                    // TOKEN_ASYNC
    { NULL,             NULL, -1 },                                    // TOKEN_BREAK
    { NULL,             NULL, -1 },                                    // TOKEN_CASE
    { NULL,             NULL, -1 },                                    // TOKEN_CATCH
    { NULL,             NULL, -1 },                                    // TOKEN_DEF
    { NULL,             NULL, -1 },                                    // TOKEN_DEFCLASS
    { NULL,             NULL, -1 },                                    // TOKEN_DO
    { NULL,             NULL, -1 },                                    // TOKEN_ELSE
    { NULL,             NULL, -1 },                                    // TOKEN_END
    { &Parser::boolean, NULL, -1 },                                    // TOKEN_FALSE
    { &Parser::function,NULL, -1 },                                    // TOKEN_FN
    { NULL,             NULL, -1 },                                    // TOKEN_FOR
    { NULL,             NULL, -1 },                                    // TOKEN_IF
    { NULL,             NULL, -1 },                                    // TOKEN_IMPORT
    { NULL,             NULL, -1 },                                    // TOKEN_IN
    { NULL,             &Parser::is, PRECEDENCE_IS },                  // TOKEN_IS
    { NULL,             NULL, -1 },                                    // TOKEN_MATCH
    { &Parser::not_,    NULL, -1 },                                    // TOKEN_NOT
    { &Parser::nothing, NULL, -1 },                                    // TOKEN_NOTHING
    { NULL,             &Parser::or_, PRECEDENCE_LOGICAL },            // TOKEN_OR
    { NULL,             NULL, -1 },                                    // TOKEN_RETURN
    { NULL,             NULL, -1 },                                    // TOKEN_THEN
    { &Parser::throw_,  NULL, -1 },                                    // TOKEN_THROW
    { &Parser::boolean, NULL, -1 },                                    // TOKEN_TRUE
    { NULL,             NULL, -1 },                                    // TOKEN_VAL
    { NULL,             NULL, -1 },                                    // TOKEN_VAR
    { NULL,             NULL, -1 },                                    // TOKEN_WHILE
    { NULL,             NULL, -1 },                                    // TOKEN_XOR

    { &Parser::record,  NULL, -1 },                                    // TOKEN_FIELD
    { &Parser::name,    &Parser::call, PRECEDENCE_CALL },              // TOKEN_NAME
    { &Parser::number,  NULL, -1 },                                    // TOKEN_NUMBER
    { &Parser::character, NULL, -1 },                                    // TOKEN_CHARACTER
    { &Parser::string,  NULL, -1 },                                    // TOKEN_STRING

    { NULL,             NULL, -1 },                                    // TOKEN_LINE
    { NULL,             NULL, -1 },                                    // TOKEN_ERROR
    { NULL,             NULL, -1 }                                     // TOKEN_EOF
  };

  gc<ModuleAst> Parser::parseModule()
  {
    Array<gc<Expr> > exprs;

    do
    {
      if (lookAhead(TOKEN_EOF)) break;
      exprs.add(topLevelExpression());
    }
    while (match(TOKEN_LINE));

    consume(TOKEN_EOF, "Expected end of file.");

    // An empty module is equivalent to `nothing`.
    if (exprs.count() == 0)
    {
      exprs.add(new NothingExpr(last()->pos()));
    }

    return new ModuleAst(new SequenceExpr(spanFrom(exprs[0]), exprs));
  }

  gc<Expr> Parser::parseExpression()
  {
    return topLevelExpression();
  }

  gc<Expr> Parser::parseBlock(TokenType endToken)
  {
    TokenType dummy;
    return parseBlock(true, endToken, endToken, endToken, &dummy);
  }

  gc<Expr> Parser::parseBlock(TokenType end1, TokenType end2,
                              TokenType* outEndToken)
  {
    return parseBlock(true, end1, end2, end2, outEndToken);
  }

  gc<Expr> Parser::parseBlock(TokenType end1, TokenType end2, TokenType end3,
                              TokenType* outEndToken)
  {
    return parseBlock(true, end1, end2, end3, outEndToken);
  }
  
  gc<Expr> Parser::parseBlock(bool allowCatch, TokenType end1, TokenType end2,
                              TokenType end3, TokenType* outEndToken)
  {
    // If we have a newline, then it's an actual block, otherwise it's a
    // single expression.
    if (match(TOKEN_LINE))
    {
      Array<gc<Expr> > exprs;

      do
      {
        if (lookAhead(end1)) break;
        if (lookAhead(end2)) break;
        if (lookAhead(end3)) break;
        if (lookAhead(TOKEN_CATCH)) break;

        gc<Expr> expr = statementLike();
        if (expr.isNull()) break;
        exprs.add(expr);
      }
      while (match(TOKEN_LINE));
      
      if (lookAhead(TOKEN_EOF))
      {
        checkForMissingLine();
        reporter_.error(current()->pos(), "Unterminated block.");
      }
      
      // Return which kind of token we ended the block with, for callers that
      // care.
      *outEndToken = current()->type();

      // If the block ends with 'end', then we want to consume that token,
      // otherwise we want to leave it unconsumed to be consistent with the
      // single-expression block case.
      match(TOKEN_END);

      gc<Expr> block = createSequence(exprs);

      // Parse any catch clauses.
      if (allowCatch)
      {
        Array<MatchClause> catches;
        while (match(TOKEN_CATCH))
        {
          gc<Pattern> pattern = parsePattern(false);
          consume(TOKEN_THEN, "Expect 'then' after catch pattern.");
          gc<Expr> body = parseBlock(false, end1, end2, end3, outEndToken);
          catches.add(MatchClause(pattern, body));
        }

        if (catches.count() > 0)
        {
          block = new CatchExpr(spanFrom(block), block, catches);
        }
      }

      return block;
    }
    else if (lookAhead(TOKEN_EOF))
    {
      checkForMissingLine();
      reporter_.error(current()->pos(),
          "Expected block or expression but reached end of file.");
      
      // Return a fake node so we can continue and report errors.
      return new NothingExpr(current()->pos());
    }
    else
    {
      // Not a block, so no block end token.
      *outEndToken = TOKEN_EOF;
      return statementLike();
    }
  }
  
  gc<Expr> Parser::topLevelExpression()
  {
    gc<Token> start = current();
    
    if (match(TOKEN_DEF))
    {
      // Examples:
      // Infix:              def (haystack) contains(needle)
      // No left arg:        def print(text)
      // No right arg:       def (text) reverse
      // Setter:             def (person) name = (name)
      // Setter with arg:    def (list) at(index) = (item)
      // Indexer:            def (string)[index]
      // Index setter:       def (string)[index] = (char)

      gc<Pattern> leftParam;
      gc<String> name;
      gc<Pattern> rightParam;
      gc<Pattern> value;
      
      if (match(TOKEN_LEFT_PAREN))
      {
        leftParam = parsePattern(true);
        consume(TOKEN_RIGHT_PAREN, "Expect ')' after pattern.");
      }
      
      if (lookAhead(TOKEN_NAME) ||
          lookAhead(TOKEN_DOTDOT) ||
          lookAhead(TOKEN_DOTDOTDOT) ||
          lookAhead(TOKEN_EQEQ) ||
          lookAhead(TOKEN_NEQ) ||
          lookAhead(TOKEN_COMPARE_OP) ||
          lookAhead(TOKEN_TERM_OP) ||
          lookAhead(TOKEN_PRODUCT_OP))
      {
        name = consume()->text();
        
        if (match(TOKEN_LEFT_PAREN))
        {
          if (match(TOKEN_RIGHT_PAREN))
          {
            // Allow () empty pattern to just mean "no parameter".
            // TODO(bob): Do we want to keep this? Seems unsymmetric and
            // pointless.
          }
          else
          {
            rightParam = parsePattern(true);
            consume(TOKEN_RIGHT_PAREN, "Expect ')' after pattern.");
          }
        }
      }
      else if (match(TOKEN_LEFT_BRACKET))
      {
        // It's an indexer.
        name = String::create("[]");
        
        rightParam = parsePattern(true);
        consume(TOKEN_RIGHT_BRACKET, "Except ']' after indexer pattern.");
      }
      else
      {
        reporter_.error(current()->pos(),
            "Expect a method name or pattern after 'def' but got '%s'.",
            current()->text()->cString());
      }
      
      // See if it's a setter.
      if (match(TOKEN_EQ))
      {
        consume(TOKEN_LEFT_PAREN, "Expect '(' after '=' to define setter.");
        value = parsePattern(true);
        consume(TOKEN_RIGHT_PAREN, "Expect ')' after value pattern.");
      }
      
      // See if this is a native method.
      gc<Expr> body;
      if (lookAhead(TOKEN_NAME) && (*current()->text() == "native"))
      {
        consume();
        gc<Token> text = consume(TOKEN_STRING,
                                 "Expect string after 'native'.");
        body = new NativeExpr(text->pos(), text->text());
      }
      else
      {
        body = parseBlock();
      }
      
      return new DefExpr(spanFrom(start), leftParam, name, rightParam, value,
                         body);
    }
    
    if (match(TOKEN_DEFCLASS))
    {
      gc<Token> name = consume(TOKEN_NAME,
                               "Expect name after 'defclass'.");

      Array<gc<Expr> > superclasses;
      if (match(TOKEN_IS))
      {
        do
        {
          superclasses.add(parsePrecedence(PRECEDENCE_CALL));
        }
        while (match(TOKEN_COMMA));
      }

      bool isNative = false;
      Array<gc<ClassField> > fields;

      if (lookAhead(TOKEN_NAME) && (*current()->text() == "native"))
      {
        consume();
        isNative = true;
      }
      else
      {
        consume(TOKEN_LINE, "Expect newline after class name.");

        while (true)
        {
          if (match(TOKEN_VAR) || match(TOKEN_VAL))
          {
            bool isMutable = last()->is(TOKEN_VAR);
            gc<String> name = consume(TOKEN_NAME, "Expect field name.")->text();
            gc<Pattern> pattern;
            gc<Expr> initializer;

            if (match(TOKEN_IS))
            {
              gc<Expr> type = parseExpressionInPattern(true);
              pattern = new TypePattern(type->pos(), type);
            }
            
            if (match(TOKEN_EQ))
            {
              initializer = flowControl();
            }
            
            consume(TOKEN_LINE, "Expect newline after class field.");
            
            fields.add(new ClassField(isMutable, name, pattern, initializer));
          }
          else
          {
            consume(TOKEN_END, "Expect 'end' after class fields.");
            break;
          }
        }
      }
      
      return new DefClassExpr(spanFrom(start), name->text(), isNative,
                              superclasses, fields);
    }

    if (match(TOKEN_IMPORT))
    {
      Array<gc<String> > names;
      do
      {
        const char* firstError = "Expect name after 'import'.";
        const char* restError = "Expect name after '.' in import.";
        gc<Token> namePart = consume(TOKEN_NAME,
            names.count() == 0 ? firstError : restError);
        names.add(namePart->text());
      } while (match(TOKEN_DOT));

      // TODO(bob): Create String::join() to optimize this?
      gc<String> name = names[0];
      for (int i = 1; i < names.count(); i++)
      {
        name = String::format("%s.%s", name->cString(), names[i]->cString());
      }

      return new ImportExpr(spanFrom(start), name);
    }
    
    return statementLike();
  }
    
  gc<Expr> Parser::statementLike()
  {
    gc<Token> start = current();

    if (match(TOKEN_ASYNC))
    {
      gc<Expr> body = parseBlock();
      return new AsyncExpr(spanFrom(start), body);
    }

    if (match(TOKEN_BREAK))
    {
      // TODO(bob): Allow multiple sequential breaks ("break break ...") to
      // exit multiple nexted loops?
      return new BreakExpr(spanFrom(start));
    }
    
    if (match(TOKEN_DEF))
    {
      // Methods can only be declared at the top level. Show a friendly error.
      reporter_.error(current()->pos(),
          "Methods can only be declared at the top level of a module.");
    }
    
    if (match(TOKEN_RETURN))
    {
      // Parse the value if there is one.
      gc<Expr> value;
      if (!lookAhead(TOKEN_LINE))
      {
        value = flowControl();
      }

      return new ReturnExpr(spanFrom(start), value);
    }

    if (match(TOKEN_VAR) || match(TOKEN_VAL))
    {
      bool isMutable = last()->is(TOKEN_VAR);

      gc<Pattern> pattern = parsePattern(false);
      consume(TOKEN_EQ, "Expect '=' after variable declaration.");
      gc<Expr> value = flowControl();

      return new VariableExpr(spanFrom(start), isMutable, pattern, value);
    }

    return flowControl();
  }

  gc<Expr> Parser::flowControl()
  {
    gc<Token> start = current();
    
    if (match(TOKEN_DO))
    {
      gc<Expr> body = parseBlock();
      return new DoExpr(spanFrom(start), body);
    }
    
    if (match(TOKEN_FOR))
    {
      gc<Pattern> pattern = parsePattern(false);
      consume(TOKEN_IN, "Expect 'in' after for loop pattern.");
      gc<Expr> iterator = parseBlock(TOKEN_DO);
      consume(TOKEN_DO, "Expect 'do' after for loop iterator.");
      gc<Expr> body = parseBlock();
      
      return new ForExpr(spanFrom(start), pattern, iterator, body);
    }

    if (match(TOKEN_IF))
    {
      gc<Expr> condition = parseBlock(TOKEN_THEN);
      consume(TOKEN_THEN, "Expect 'then' after 'if' condition.");

      TokenType endToken;
      gc<Expr> thenArm = parseBlock(TOKEN_ELSE, TOKEN_END, &endToken);
      gc<Expr> elseArm;

      // Don't look for an else arm if the then arm was a block that
      // specifically ended with 'end'.
      if ((endToken != TOKEN_END) && match(TOKEN_ELSE))
      {
        elseArm = parseBlock();
      }

      return new IfExpr(spanFrom(start), condition, thenArm, elseArm);
    }

    if (match(TOKEN_MATCH))
    {
      // Parse the value.
      gc<Expr> value = parsePrecedence(PRECEDENCE_ASSIGNMENT);

      // Require a newline between the value and the first case.
      consume(TOKEN_LINE, "Expect a newline after a match's value expression.");

      // Parse the cases.
      Array<MatchClause> cases;
      while (match(TOKEN_CASE))
      {
        gc<Pattern> pattern = parsePattern(false);

        consume(TOKEN_THEN, "Expect 'then' after a case pattern.");

        TokenType endToken;
        gc<Expr> body = parseBlock(TOKEN_ELSE, TOKEN_END, TOKEN_CASE,
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

      // Parse the else.
      if (match(TOKEN_ELSE))
      {
        gc<Expr> body = parseBlock();
        cases.add(MatchClause(gc<Pattern>(), body));
      }

      consume(TOKEN_LINE,
              "Expect newline after last case in a match expression.");
      consume(TOKEN_END, "Expect 'end' after match expression.");

      return new MatchExpr(spanFrom(start), value, cases);
    }

    if (match(TOKEN_WHILE))
    {
      gc<Expr> condition = parseBlock(TOKEN_DO);
      consume(TOKEN_DO, "Expect 'do' after 'while' condition.");
      gc<Expr> body = parseBlock();
      
      return new WhileExpr(spanFrom(start), condition, body);
    }
    
    return parsePrecedence();
  }

  gc<Expr> Parser::parsePrecedence(int precedence)
  {
    gc<Token> token = consume();
    PrefixParseFn prefix = expressions_[token->type()].prefix;

    if (prefix == NULL)
    {
      gc<String> tokenText = token->toString();
      reporter_.error(token->pos(), "Unexpected token '%s'.",
                      tokenText->cString());
      
      // Return a fake expression so we can keep parsing to find more errors.
      return new NothingExpr(token->pos());
    }

    gc<Expr> left = (this->*prefix)(token);

    while (precedence <= expressions_[current()->type()].precedence)
    {
      token = consume();
      InfixParseFn infix = expressions_[token->type()].infix;
      left = (this->*infix)(left, token);
    }

    return left;
  }

  // Prefix parsers -----------------------------------------------------------

  gc<Expr> Parser::boolean(gc<Token> token)
  {
    return new BoolExpr(token->pos(), token->type() == TOKEN_TRUE);
  }

  gc<Expr> Parser::character(gc<Token> token)
  {
    // TODO(bob): Handle non-literal characters.
    return new CharacterExpr(token->pos(), (*token->text())[0]);
  }

  gc<Expr> Parser::function(gc<Token> token)
  {
    consume(TOKEN_LEFT_PAREN, "Expect '(' after 'fn'.");

    // Allow an empty pattern.
    gc<Pattern> pattern;
    if (!match(TOKEN_RIGHT_PAREN))
    {
      pattern = parsePattern(false);
      consume(TOKEN_RIGHT_PAREN, "Expect ')' after function pattern.");
    }

    ASSERT(pattern.isNull(), "Functions with parameters aren't implemented yet.");

    gc<Expr> body = parseBlock();
    return new FnExpr(spanFrom(token), pattern, body);
  }

  gc<Expr> Parser::group(gc<Token> token)
  {
    gc<Expr> expr = flowControl();
    consume(TOKEN_RIGHT_PAREN, "Expect ')'.");
    return expr;
  }
  
  gc<Expr> Parser::list(gc<Token> token)
  {
    Array<gc<Expr> > elements;
    
    // Handle empty lists.
    if (!lookAhead(TOKEN_RIGHT_BRACKET))
    {
      while (true)
      {
        // TODO(bob): What about parse errors or EOF?
        elements.add(parsePrecedence(PRECEDENCE_LOGICAL));
        if (!match(TOKEN_COMMA)) break;
      }
    }
    
    consume(TOKEN_RIGHT_BRACKET, "Except ']' to close list.");
    return new ListExpr(spanFrom(token), elements);
  }
  
  gc<Expr> Parser::name(gc<Token> token)
  {
    return call(gc<Expr>(), token);
  }

  gc<Expr> Parser::not_(gc<Token> token)
  {
    gc<Expr> value = parsePrecedence(PRECEDENCE_CALL);
    return new NotExpr(spanFrom(token), value);
  }

  gc<Expr> Parser::nothing(gc<Token> token)
  {
    return new NothingExpr(token->pos());
  }

  gc<Expr> Parser::number(gc<Token> token)
  {
    double number = atof(token->text()->cString());
    return new NumberExpr(token->pos(), number);
  }

  gc<Expr> Parser::record(gc<Token> token)
  {
    Array<Field> fields;

    gc<Expr> value = parsePrecedence(PRECEDENCE_RECORD + 1);
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

      // Make sure there are no duplicate fields.
      for (int j = 0; j < fields.count(); j++)
      {
        if (*name == *fields[j].name)
        {
          reporter_.error(current()->pos(),
                          "Cannot use field '%s' twice in a record.",
                          name->cString());
        }
      }

      value = parsePrecedence(PRECEDENCE_RECORD + 1);
      fields.add(Field(name, value));

      i++;
    }

    return new RecordExpr(spanFrom(token), fields);
  }

  gc<Expr> Parser::string(gc<Token> token)
  {
    return new StringExpr(token->pos(), token->text());
  }

  gc<Expr> Parser::throw_(gc<Token> token)
  {
    gc<Expr> value = parsePrecedence(PRECEDENCE_LOGICAL);
    return new ThrowExpr(spanFrom(token), value);
  }

  // Infix parsers ------------------------------------------------------------

  gc<Expr> Parser::and_(gc<Expr> left, gc<Token> token)
  {
    gc<Expr> right = parsePrecedence(expressions_[token->type()].precedence);
    return new AndExpr(token->pos(), left, right);
  }
  
  gc<Expr> Parser::assignment(gc<Expr> left, gc<Token> token)
  {
    gc<LValue> lvalue = convertToLValue(left);
    gc<Expr> value = parsePrecedence(PRECEDENCE_ASSIGNMENT);
    return new AssignExpr(spanFrom(left), lvalue, value);
  }
  
  gc<Expr> Parser::binaryOp(gc<Expr> left, gc<Token> token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    gc<Expr> right = parsePrecedence(expressions_[token->type()].precedence);

    return new BinaryOpExpr(token->pos(), left, token->type(), right);
  }

  gc<Expr> Parser::call(gc<Expr> left, gc<Token> token)
  {
    // See if we have an argument on the right.
    bool hasRightArg = false;
    gc<Expr> right;
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
      return new NameExpr(spanFrom(token), token->text());
    }

    return new CallExpr(spanFrom(token), left, token->text(), right);
  }
    
  gc<Expr> Parser::infixCall(gc<Expr> left, gc<Token> token)
  {
    // TODO(bob): Support right-associative infix. Needs to do precedence
    // - 1 here, to be right-assoc.
    gc<Expr> right = parsePrecedence(
        expressions_[token->type()].precedence + 1);
    
    return new CallExpr(spanFrom(left), left, token->text(), right);
  }
  
  gc<Expr> Parser::infixRecord(gc<Expr> left, gc<Token> token)
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

      // Make sure there are no duplicate fields.
      for (int j = 0; j < fields.count(); j++)
      {
        if (*name == *fields[j].name)
        {
          reporter_.error(current()->pos(),
                          "Cannot use field '%s' twice in a record.",
                          name->cString());
        }
      }

      gc<Expr> value = parsePrecedence(PRECEDENCE_RECORD + 1);
      fields.add(Field(name, value));

      i++;
    }
    while (match(TOKEN_COMMA));

    return new RecordExpr(spanFrom(left), fields);
  }

  gc<Expr> Parser::is(gc<Expr> left, gc<Token> token)
  {
    gc<Expr> type = parsePrecedence(PRECEDENCE_CALL);
    return new IsExpr(spanFrom(left), left, type);
  }

  gc<Expr> Parser::or_(gc<Expr> left, gc<Token> token)
  {
    gc<Expr> right = parsePrecedence(expressions_[token->type()].precedence);
    return new OrExpr(spanFrom(left), left, right);
  }

  gc<Expr> Parser::subscript(gc<Expr> left, gc<Token> token)
  {
    // Parse the subscript.
    // TODO(bob): Is this right? Do we want to allow variable declarations
    // here?
    gc<Expr> subscript = statementLike();
    consume(TOKEN_RIGHT_BRACKET, "Expect ']' after subscript argument.");

    return new CallExpr(spanFrom(left), left, String::create("[]"), subscript);
  }

  // Pattern parsers ----------------------------------------------------------

  gc<Pattern> Parser::parsePattern(bool isMethod)
  {
    return recordPattern(isMethod);
  }

  gc<Pattern> Parser::recordPattern(bool isMethod)
  {
    bool hasField = false;
    gc<Token> start = current();
    Array<PatternField> fields;

    do
    {
      gc<String> name;
      if (match(TOKEN_FIELD))
      {
        name = last()->text();
        hasField = true;
      }
      else
      {
        name = String::format("%d", fields.count());
      }

      // Make sure the field isn't already in use.
      for (int j = 0; j < fields.count(); j++)
      {
        if (*name == *fields[j].name)
        {
          reporter_.error(current()->pos(),
              "Cannot use field '%s' twice in a record pattern.",
              name->cString());
        }
      }

      gc<Pattern> value = variablePattern(isMethod);

      if (value.isNull())
      {
        reporter_.error(current()->pos(), "Expect pattern.");
      }

      fields.add(PatternField(name, value));
    }
    while (match(TOKEN_COMMA));

    // If we just have a single unnamed field, it's not a record, it's just
    // that pattern.
    if (fields.count() == 1 && !hasField)
    {
      return fields[0].value;
    }

    return new RecordPattern(spanFrom(start), fields);
  }

  gc<Pattern> Parser::variablePattern(bool isMethod)
  {
    if (match(TOKEN_NAME))
    {
      gc<Token> name = last();
      if (*name->text() == "_")
      {
        return new WildcardPattern(name->pos());
      }
      else
      {
        gc<Pattern> inner = primaryPattern(isMethod);
        return new VariablePattern(spanFrom(name), name->text(), inner);
      }
    }
    else
    {
      return primaryPattern(isMethod);
    }
  }

  gc<Pattern> Parser::primaryPattern(bool isMethod)
  {
    gc<Token> start = last();

    if (match(TOKEN_TRUE) || match(TOKEN_FALSE))
    {
      gc<Expr> value = boolean(last());
      return new ValuePattern(spanFrom(start), value);
    }

    if (match(TOKEN_EQEQ))
    {
      gc<Expr> value = parseExpressionInPattern(isMethod);
      return new ValuePattern(spanFrom(start), value);
    }

    if (match(TOKEN_IS))
    {
      gc<Expr> type = parseExpressionInPattern(isMethod);
      return new TypePattern(spanFrom(start), type);
    }

    if (match(TOKEN_NOTHING))
    {
      gc<Expr> value = nothing(last());
      return new ValuePattern(spanFrom(start), value);
    }

    if (match(TOKEN_NUMBER))
    {
      gc<Expr> value = number(last());
      return new ValuePattern(spanFrom(start), value);
    }

    if (match(TOKEN_STRING))
    {
      gc<Expr> value = string(last());
      return new ValuePattern(spanFrom(start), value);
    }

    if (match(TOKEN_LEFT_PAREN))
    {
      gc<Pattern> pattern = parsePattern(isMethod);
      consume(TOKEN_RIGHT_PAREN, "Expect ')' after pattern.");
      return pattern;
    }

    return gc<Pattern>();
  }

  gc<Expr> Parser::parseExpressionInPattern(bool isMethod)
  {
    // Can only use names in method patterns.
    if (isMethod)
    {
      gc<Token> name = consume(TOKEN_NAME,
          "An expression in a method pattern can only be a simple name.");
      return new NameExpr(name->pos(), name->text());
    }

    return parsePrecedence(PRECEDENCE_COMPARISON);
  }

  // Helpers and base methods -------------------------------------------------

  gc<LValue> Parser::convertToLValue(gc<Expr> expr)
  {
    NameExpr* name = expr->asNameExpr();
    if (name != NULL)
    {
      if (*name->name() == "_")
      {
        return new WildcardLValue(name->pos());
      }
      
      return new NameLValue(name->pos(), name->name());
    }
    
    RecordExpr* record = expr->asRecordExpr();
    if (record != NULL)
    {
      Array<LValueField> fields;
      for (int i = 0; i < record->fields().count(); i++)
      {
        const Field& field = record->fields()[i];
        fields.add(LValueField(field.name, convertToLValue(field.value)));
      }
      
      return new RecordLValue(expr->pos(), fields);
    }
    
    CallExpr* call = expr->asCallExpr();
    if (call != NULL)
    {
      return new CallLValue(expr->pos(), call);
    }
    
    // If we got here, the expression is not a valid LValue.
    // TODO(bob): Better error message here.
    reporter_.error(expr->pos(), "Invalid left-hand side of assignment.");
    return gc<LValue>();
  }

  gc<Expr> Parser::createSequence(const Array<gc<Expr> >& exprs)
  {
    // If the sequence is empty, just default it to nothing.
    if (exprs.count() == 0) return new NothingExpr(last()->pos());

    // If there is just one expression in the sequence, don't wrap it.
    if (exprs.count() == 1) return exprs[0];

    return new SequenceExpr(spanFrom(exprs[0]), exprs);
  }

  const gc<Token> Parser::current()
  {
    fillLookAhead(1);
    return read_[0];
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
    if (!lookAhead(expected)) reporter_.error(current()->pos(), errorMessage);
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
    
    if (expected == TOKEN_LINE) checkForMissingLine();
    reporter_.error(current()->pos(), errorMessage);

    // Try to consume tokens until we find what we're looking for (or we run
    // out). This should reduce the number of cascaded errors caused after this
    // one.
    gc<Token> token = consume();
    while (!match(expected) && !lookAhead(TOKEN_EOF))
    {
      token = consume();
    }

    return token;
  }

  void Parser::checkForMissingLine()
  {
    if (lookAhead(TOKEN_EOF) && reporter_.numErrors() == 0)
    {
      reporter_.setNeedMoreLines();
    }
  }

  gc<SourcePos> Parser::spanFrom(gc<Token> from)
  {
    return from->pos()->spanTo(last()->pos());
  }
  
  gc<SourcePos> Parser::spanFrom(gc<Expr> from)
  {
    return from->pos()->spanTo(last()->pos());
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

