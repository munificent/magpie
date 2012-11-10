#include <fstream>
#include <iostream>
#include <string>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#define PATH_MAX MAX_PATH
#define EX_NOINPUT 1
#else
#ifdef __linux__
#include <linux/limits.h>
#else
#include <limits.h>
#endif
#include <sysexits.h>
#endif

#include "Ast.h"
#include "Compiler.h"
#include "Environment.h"
#include "Fiber.h"
#include "MagpieString.h"
#include "Object.h"
#include "Parser.h"
#include "VM.h"

using namespace magpie;
using namespace std;

// Reads a file from the given path into a String.
gc<String> readFile(const char* path)
{
  ifstream stream(path);

  if (stream.fail())
  {
    cerr << "Could not open file '" << path << "'." << endl;
    return gc<String>();
  }

  // From: http://stackoverflow.com/questions/2602013/read-whole-ascii-file-into-c-stdstring.
  string str;

  // Allocate a std::string big enough for the file.
  stream.seekg(0, ios::end);
  str.reserve(stream.tellg());
  stream.seekg(0, ios::beg);

  // Read it in.
  str.assign((istreambuf_iterator<char>(stream)),
             istreambuf_iterator<char>());

  return String::create(str.c_str());
}

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

  while (true)
  {
    gc<String> source;
    gc<Expr> expr;

    while (true)
    {
      gc<String> line = readLine(!source.isNull());
      if (source.isNull())
      {
        source = line;
      }
      else
      {
        source = String::format("%s\n%s", source->cString(), line->cString());
      }

      ErrorReporter reporter(true);
      Parser parser("<repl>", source, reporter);
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

int runFile(VM& vm, const char* fileName)
{
  gc<String> source = readFile(fileName);
  if (source.isNull()) return EX_NOINPUT;

  bool success = vm.loadModule(fileName, source);
  return success ? 0 : 1;
}

int main(int argc, const char* argv[])
{
  if (argc > 2)
  {
    // TODO(bob): Show usage, etc.
    std::cout << "magpie [script]" << std::endl;
    return 0;
  }

  char path[PATH_MAX];
  getCoreLibPath(path, PATH_MAX);

  VM vm;

  gc<String> coreSource = readFile(path);
  if (coreSource.isNull())
  {
    return 1;
  }

  vm.init(coreSource);

  if (argc == 1) return repl(vm);

  return runFile(vm, argv[1]);
}
