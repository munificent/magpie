#include "TokenTests.h"
#include "Token.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  void TokenTests::run() {
    create();
  }
  
  void TokenTests::create() {
    TestRoot roots;
    Memory memory(roots, 1000);
    AllocScope scope(memory);
    
    temp<String> text = String::create(scope, "foo");
    temp<Token> token = Token::create(scope, TOKEN_NAME, text);
    
    std::cout << token->text().cString() << std::endl;
    
    EXPECT_EQUAL(TOKEN_NAME, token->type());
    EXPECT_EQUAL(0, strcmp("foo", token->text().cString()));
  }
}

