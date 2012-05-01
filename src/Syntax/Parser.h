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
    Parser(const char* fileName, gc<String> source, ErrorReporter& reporter)
    : lexer_(fileName, source),
      reporter_(reporter)
    {}
    
    gc<ModuleAst> parseModule();
    
  private:
    typedef gc<Node> (Parser::*PrefixParseFn)(gc<Token> token);
    typedef gc<Node> (Parser::*InfixParseFn)(gc<Node> left, gc<Token> token);
    
    struct Parselet
    {
      PrefixParseFn prefix;
      InfixParseFn  infix;
      int           precedence;
    };
    
    gc<Node> parseBlock(TokenType endToken = TOKEN_END);
    gc<Node> parseBlock(TokenType end1, TokenType end2, TokenType* outEndToken);
    gc<Node> statementLike();

    // Parses an expression with the given precedence or higher.
    gc<Node> parsePrecedence(int precedence = 0);
    
    // Prefix expression parsers.
    gc<Node> boolean(gc<Token> token);
    gc<Node> name(gc<Token> token);
    gc<Node> number(gc<Token> token);
    gc<Node> string(gc<Token> token);

    // Infix expression parsers.
    gc<Node> binaryOp(gc<Node> left, gc<Token> token);

    // Pattern parsing.
    gc<Pattern> parsePattern();
    gc<Pattern> variablePattern();

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
    gc<Token> consume();
    
    // Consumes the current Token if it matches the expected type.
    // Otherwise reports the given error message and returns a null temp.
    gc<Token> consume(TokenType expected, const char* errorMessage);
    
    // Gets whether or not any errors have been reported.
    bool hadError() const { return reporter_.numErrors() > 0; }
    
    void fillLookAhead(int count);
    
    static Parselet expressions_[TOKEN_NUM_TYPES];
    
    Lexer lexer_;
    
    // The 2 here is the maximum number of lookahead tokens.
    Queue<gc<Token>, 2> read_;
    
    ErrorReporter& reporter_;
    
    NO_COPY(Parser);
  };
}
