#include "Ast.h"
#include "Compiler.h"
#include "Environment.h"
#include "Fiber.h"
#include "MagpieString.h"
#include "Object.h"
#include "Parser.h"
#include "VM.h"

using namespace magpie;

gc<String> readLine(bool isContinued)
{
  std::string line;
  while (line.size() == 0)
  {
    std::cout << (isContinued ? "| " : "> ");
    std::getline(std::cin, line);
  }

  return String::create(line.c_str());
}

int repl(VM& vm)
{
  std::cout << std::endl;
  std::cout << "      _/Oo>" << std::endl;
  std::cout << "     /__/     magpie v0.0.0" << std::endl;
  std::cout << "____//hh___________________" << std::endl;
  std::cout << "   //" << std::endl;
  std::cout << std::endl;
  std::cout << "Type 'Ctrl+C' to exit." << std::endl;

  if (!vm.initRepl())
  {
    std::cerr << "Could not initialize REPL." << std::endl;
    return 1;
  }

  while (true)
  {
    gc<String> code;
    gc<Expr> expr;

    while (true)
    {
      gc<String> line = readLine(!code.isNull());
      if (code.isNull())
      {
        code = line;
      }
      else
      {
        code = String::format("%s\n%s", code->cString(), line->cString());
      }

      gc<SourceFile> source = new SourceFile(String::create("<repl>"), code);
      
      ErrorReporter reporter(true);
      Parser parser(source, reporter);
      expr = parser.parseExpression();

      if (reporter.needMoreLines()) continue;
      if (reporter.numErrors() == 0) break;
      return 3;
    }

    // Evaluate the expression.
    gc<Object> result = vm.evaluateReplExpression(expr);

    // Don't show the result if it's a definition.
    if (expr->asDefExpr() != NULL) continue;
    if (expr->asDefClassExpr() != NULL) continue;

    std::cout << "= " << result << std::endl;
  }
}

int main(int argc, const char* argv[])
{
  if (argc > 2)
  {
    // TODO(bob): Show usage, etc.
    std::cout << "magpie [script]" << std::endl;
    return 1;
  }

  VM vm;

  if (argc == 1) return repl(vm);

  bool success = vm.runProgram(String::create(argv[1]));
  return success ? 0 : 1;
}
