#include "Syntax/Ast.h"
#include "ParserTests.h"
#include "Memory/Memory.h"
#include "Syntax/Parser.h"
#include "Memory/RootSource.h"

namespace magpie
{
  void ParserTests::runTests()
  {
    parseModule();
  }

  void ParserTests::parseModule()
  {
    gc<String> code = String::create("def foo()\n1+2*3 and 4/5+6%7\nend");
    ErrorReporter reporter;
    gc<SourceFile> source = new SourceFile(String::create("<file>"), code);
    Parser parser(source, reporter);
    gc<ModuleAst> module = parser.parseModule();

    gc<String> text = module->toString();
    EXPECT_EQUAL("((1 + (2 * 3)) and ((4 / 5) + (6 % 7)))", *text);
  }
}

