#include "TokenTests.h"
#include "Token.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie
{
  void TokenTests::runTests()
  {
    create();
    is();
  }

  void TokenTests::create()
  {
    AllocScope scope;

    temp<String> text = String::create("foo");
    temp<Token> token = Token::create(TOKEN_NAME, text);

    EXPECT_EQUAL(TOKEN_NAME, token->type());
    EXPECT_EQUAL("foo", *token->text());
  }
  
  void TokenTests::is()
  {
    AllocScope scope;
    
    temp<Token> token = Token::create(TOKEN_NAME, String::create("foo"));
    
    EXPECT(token->is(TOKEN_NAME));
    EXPECT_FALSE(token->is(TOKEN_NUMBER));
  }
}

