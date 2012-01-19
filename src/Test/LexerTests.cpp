#include "LexerTests.h"
#include "Lexer.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  void LexerTests::run() {
    create();
  }
  
  void LexerTests::create() {
    TestRoot root;
    Memory memory(root, 1000);
    AllocScope scope(memory);

    temp<String> source = String::create(scope, "1 + (foo)");
    Lexer lexer(source);
    
    temp<Token> token = lexer.readToken(scope);
    EXPECT_EQUAL(TOKEN_NUMBER, token->type());
    EXPECT_EQUAL(0, strcmp("1", token->text().cString()));

    token = lexer.readToken(scope);
    EXPECT_EQUAL(TOKEN_PLUS, token->type());
    EXPECT_EQUAL(0, strcmp("+", token->text().cString()));

    token = lexer.readToken(scope);
    EXPECT_EQUAL(TOKEN_LEFT_PAREN, token->type());
    EXPECT_EQUAL(0, strcmp("(", token->text().cString()));
    
    token = lexer.readToken(scope);
    EXPECT_EQUAL(TOKEN_NAME, token->type());
    EXPECT_EQUAL(0, strcmp("foo", token->text().cString()));
    
    token = lexer.readToken(scope);
    EXPECT_EQUAL(TOKEN_RIGHT_PAREN, token->type());
    EXPECT_EQUAL(0, strcmp(")", token->text().cString()));
  }
}

