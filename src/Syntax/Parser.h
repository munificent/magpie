#pragma once

#include "Macros.h"
#include "ErrorReporter.h"
#include "Lexer.h"
#include "Queue.h"

namespace magpie
{
  class Lexer;
  class Node;
  class ErrorReporter;
  
  // Parses Magpie source from a string into an abstract syntax tree. The
  // implementation is basically a vanilla recursive descent parser wrapped
  // around a Pratt operator precedence parser for handling expressions.
  class Parser
  {
  public:
    // TODO(bob): Need to do something better for the strings here. Since the
    // Parser class isn't GC'd, it can't point to stuff that is.
    Parser(const char* fileName, gc<String> source, ErrorReporter& reporter)
    : lexer_(fileName, source),
      reporter_(reporter)
    {}
    
    ModuleAst* parseModule();
    
  private:
    typedef Node* (Parser::*PrefixParseFn)(Token* token);
    typedef Node* (Parser::*InfixParseFn)(Node* left, Token* token);
    
    struct Parselet
    {
      PrefixParseFn prefix;
      InfixParseFn  infix;
      int           precedence;
    };
    
    Node* parseBlock();
    Node* statementLike();

    // Parses an expression with the given precedence or higher.
    Node* parsePrecedence(int precedence = 0);
    
    // Prefix expression parsers.
    Node* boolean(Token* token);
    Node* name(Token* token);
    Node* number(Token* token);
    Node* string(Token* token);

    // Infix expression parsers.
    Node* binaryOp(Node* left, Token* token);

    // Pattern parsing.
    Pattern* parsePattern();
    Pattern* variablePattern();

    // Gets the Token the parser is currently looking at.
    const Token& current();
    
    // Returns true if the current Token is the given type.
    bool lookAhead(TokenType type);
    
    // Returns true if the current and next Tokens is the given types (in
    // order).
    bool lookAhead(TokenType current, TokenType next);
    
    // Consumes the current Token and returns true if it is the given type,
    // otherwise returns false.
    bool match(TokenType type);
    
    // Verifies the current Token if it matches the expected type, and
    // reports an error if it doesn't. Does not consume the token either
    // way.
    void expect(TokenType expected, const char* errorMessage);
    
    // TODO(bob): Return temp or gc?
    // Consumes the current Token and advances the Parser.
    Token* consume();
    
    // Consumes the current Token if it matches the expected type.
    // Otherwise reports the given error message and returns a null temp.
    Token* consume(TokenType expected, const char* errorMessage);
    
    // Gets whether or not any errors have been reported.
    bool hadError() const { return reporter_.numErrors() > 0; }
    
    void fillLookAhead(int count);
    
    static Parselet expressions_[TOKEN_NUM_TYPES];
    
    Lexer lexer_;
    
    // The 2 here is the maximum number of lookahead tokens.
    Queue<Token*, 2> read_;
    
    ErrorReporter& reporter_;
    
    NO_COPY(Parser);
  };
}
