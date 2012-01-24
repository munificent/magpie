#include "ParserTests.h"
#include "Memory.h"
#include "Node.h"
#include "Parser.h"
#include "RootSource.h"

namespace magpie
{
  void ParserTests::runTests()
  {
    parseExpression();
  }

  void ParserTests::parseExpression()
  {
    AllocScope scope;

    temp<String> source = String::create("1+2*3 and 4/5+6%7");
    Parser parser(source);
    temp<Node> node = parser.parseExpression();
    
    temp<String> text = node->toString();
    
    EXPECT_EQUAL("((1 + (2 * 3)) and ((4 / 5) + (6 % 7)))", *text);
  }
}

