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
    gc<String> source = String::create("def foo()\n1+2*3 and 4/5+6%7\nend");
    ErrorReporter reporter;
    Parser parser("", source, reporter);
    ModuleAst* module = parser.parseModule();
    
    // TODO(bob): Re-enable once AST supports pretty-printing again.
    /*
    gc<String> text = module->toString();
    
    EXPECT_EQUAL("((1 + (2 * 3)) and ((4 / 5) + (6 % 7)))", *text);
    */
    
    delete module;
  }
}

