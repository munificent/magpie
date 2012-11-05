#include <fstream>
#include <iostream>
#include <string>
#include <cstring>
#include <stdint.h>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#define PATH_MAX MAX_PATH
#define EX_NOINPUT 1
#else
#include <sysexits.h>
#endif

#if defined(__APPLE__)
#include <mach-o/dyld.h>
#elif defined(__linux__)
#include <unistd.h>
#include <linux/limits.h>
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
#if defined(__APPLE__)
  uint32_t size = PATH_MAX;
  int result = _NSGetExecutablePath(relativePath, &size);
  ASSERT(result == 0, "Executable path too long.");
#elif defined(_WIN32)
  GetModuleFileName(NULL, relativePath, PATH_MAX);
  ASSERT(GetLastError() != ERROR_INSUFFICIENT_BUFFER, "Executable path too long.")
#elif defined(__linux__)
  int len = readlink("/proc/self/exe", relativePath, PATH_MAX-1);
  ASSERT(len != -1, "Executable path too long.");
  relativePath[len] = '\0';
#else
#error Platform not yet supported!
#endif

  // Cut off file name from path
  char* lastSep = NULL;
  for (char* c = relativePath; *c != '\0'; c++)
  {
#if defined(_WIN32)
    if (*c == '/' || *c == '\\')
#else
    if (*c == '/')
#endif
    {
      lastSep = c;
    }
  }

  if (lastSep != NULL)
  {
    *lastSep = '\0';
  }

  // Find the magpie main directory relative to the executable.
  // TODO(bob): Hack. Try to work from the build directory too.
#if defined(__APPLE__)
  if (strstr(relativePath, "build/Debug") != 0 ||
      strstr(relativePath, "build/Release") != 0)
  {
    strncat(relativePath, "/../..", PATH_MAX);
  }
#elif defined(__linux__)
  if (strstr(relativePath, "1/out") != 0)
  {
    strncat(relativePath, "/../../..", PATH_MAX);
  }
#elif defined(_WIN32)
  if (strstr(relativePath, "Debug") != 0 ||
      strstr(relativePath, "Release") != 0)
  {
    strncat(relativePath, "/..", PATH_MAX);
  }
#endif

  // Add library path.
  strncat(relativePath, "/core/core.mag", PATH_MAX);

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
