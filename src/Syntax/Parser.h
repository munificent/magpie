#pragma once

#include "Macros.h"
#include "Lexer.h"
#include "Queue.h"

namespace magpie
{
  class Lexer;
  class Node;
  
  // Base class for a generic recursive descent parser.
  class Parser
  {
  public:
    Parser(gc<String> source /*, ErrorReporter & errorReporter*/)
    : lexer_(source),
      hadError_(false)
    {}
    
    // Parses an expression with the given precedence or higher.
    temp<Node> parseExpression(int precedence = 0);
    
  private:
    // Prefix parsers.
    temp<Node> number(temp<Token> token);

    // Infix parsers.
    temp<Node> binaryOp(temp<Node> left, temp<Token> token);

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
    temp<Token> consume();
    
    // Consumes the current Token if it matches the expected type.
    // Otherwise reports the given error message and returns a null temp.
    temp<Token> consume(TokenType expected, const char* errorMessage);
    
    // Reports the given error message relevant to the current token.
    void error(const char* message);
    
    // Gets whether or not any errors have been reported.
    bool hadError() const { return hadError_; }
    
  private:
    typedef temp<Node> (Parser::*PrefixParseFn)(temp<Token> token);
    typedef temp<Node> (Parser::*InfixParseFn)(temp<Node> left, temp<Token> token);
    
    struct InfixParser
    {
      InfixParseFn fn;
      int          precedence;
    };
    
    void fillLookAhead(int count);
    
    static PrefixParseFn prefixParsers_[TOKEN_NUM_TYPES];
    static InfixParser   infixParsers_[TOKEN_NUM_TYPES];
    
    Lexer lexer_;
    
    // The 2 here is the maximum number of lookahead tokens.
    Queue<gc<Token>, 2> read_;
    
    bool hadError_;
    
    NO_COPY(Parser);
  };
}
