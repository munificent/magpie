#include "LexerTests.h"
#include "Lexer.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie
{
  void LexerTests::runTests()
  {
    create();
    stringLiteral();
  }

  void LexerTests::create()
  {
    gc<String> source = String::create("()[]{}");
    gc<String> file = String::create("<file>");
    Lexer lexer(file, source);

    gc<Token> token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_LEFT_PAREN, token->type());
    EXPECT_EQUAL("(", *token->text());

    token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_RIGHT_PAREN, token->type());
    EXPECT_EQUAL(")", *token->text());

    token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_LEFT_BRACKET, token->type());
    EXPECT_EQUAL("[", *token->text());

    token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_RIGHT_BRACKET, token->type());
    EXPECT_EQUAL("]", *token->text());

    token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_LEFT_BRACE, token->type());
    EXPECT_EQUAL("{", *token->text());

    token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_RIGHT_BRACE, token->type());
    EXPECT_EQUAL("}", *token->text());
  }
  
  void LexerTests::stringLiteral()
  {
    gc<String> source = String::create("\"st\\nr\"");
    gc<String> file = String::create("<file>");
    Lexer lexer(file, source);
    
    gc<Token> token = lexer.readToken();
    EXPECT_EQUAL(TOKEN_STRING, token->type());
    EXPECT_EQUAL("st\nr", *token->text());
  }
}

