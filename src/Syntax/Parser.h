#pragma once

#include "Macros.h"
#include "Lexer.h"
#include "Queue.h"

namespace magpie
{
  class Lexer;
  
  // Base class for a generic recursive descent parser.
  class Parser
  {
  protected:
    Parser(Lexer& lexer /*, ErrorReporter & errorReporter*/)
    : lexer_(lexer),
      hadError_(false)
    {}
    
    // Gets the Token the parser is currently looking at.
    const Token& current() { return *read_[0]; }
    
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
    void fillLookAhead(int count);
    
    Lexer& lexer_;
    
    // The 2 here is the maximum number of lookahead tokens.
    Queue<temp<Token>, 2> read_;
    
    bool hadError_;
    
    NO_COPY(Parser);
  };
}
