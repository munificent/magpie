#include "TokenTests.h"
#include "Token.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie
{
  void TokenTests::run()
  {
    create();
  }

  void TokenTests::create()
  {
    AllocScope scope;

    temp<String> text = String::create("foo");
    temp<Token> token = Token::create(TOKEN_NAME, text);

    EXPECT_EQUAL(TOKEN_NAME, token->type());
    EXPECT_EQUAL("foo", token->text());
  }
}

