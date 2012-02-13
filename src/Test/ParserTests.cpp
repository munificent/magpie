#include "ParserTests.h"
#include "Memory.h"
#include "Node.h"
#include "Parser.h"
#include "RootSource.h"

namespace magpie
{
  void ParserTests::runTests()
  {
    parseModule();
  }

  void ParserTests::parseModule()
  {
    AllocScope scope;

    temp<String> source = String::create("def foo()\n1+2*3 and 4/5+6%7\nend");
    Parser parser(source);
    temp<ModuleAst> module = parser.parseModule();
    
    temp<String> text = module->toString();
    
    EXPECT_EQUAL("((1 + (2 * 3)) and ((4 / 5) + (6 % 7)))", *text);
  }
}

