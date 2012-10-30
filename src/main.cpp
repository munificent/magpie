#include <fstream>
#include <iostream>
#include <string>
#include <stdint.h>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#define PATH_MAX MAX_PATH
#define EX_NOINPUT 1
#else
#include <sysexits.h>
#endif

#ifdef __APPLE__
#include <mach-o/dyld.h>
#endif

#include "Ast.h"
#include "Compiler.h"
#include "Fiber.h"
#include "MagpieString.h"
#include "Object.h"
#include "Parser.h"
#include "VM.h"

using namespace magpie;
using namespace std;

// Finds the path to the core lib.
void getCoreLibPath(char* path)
{
  char relativePath[PATH_MAX];

  // TODO(bob): Move platform-specific stuff out to another file.
  uint32_t size = PATH_MAX;
#if defined(__APPLE__)
  int result = _NSGetExecutablePath(relativePath, &size);
  ASSERT(result == 0, "Executable path too long.");
#elif defined(_WIN32)
  char dirPath[PATH_MAX];
  GetModuleFileName(NULL, dirPath, size);
  ASSERT(GetLastError() != ERROR_INSUFFICIENT_BUFFER, "Executable path too long.")
  _splitpath(dirPath, relativePath, relativePath+2, NULL, NULL);
#else
#error Platform not yet supported!
#endif

  // Find the core lib relative to the executable.
  // TODO(bob): Hack. Try to work from the build directory too.
  if (strstr(relativePath, "build/Debug/magpie") != 0 ||
      strstr(relativePath, "build/Release/magpie") != 0)
  {
    strncat(relativePath, "/../../../core/core.mag", PATH_MAX);
  }
  else
  {
    strncat(relativePath, "/../core/core.mag", PATH_MAX);
  }

  // Canonicalize the path.
#ifdef _WIN32
  _fullpath(path, relativePath, PATH_MAX);
#else
  realpath(relativePath, path);
#endif
}

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
  getCoreLibPath(path);

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
