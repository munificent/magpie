#include <fstream>
#include <iostream>

#include "Ast.h"
#include "Compiler.h"
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
    cout << "Could not open file '" << path << "'." << endl;
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

int repl()
{
  VM vm;
  vm.init();
  
  while (true)
  {
    gc<String> source;
    gc<Expr> ast;
    
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
      ast = parser.parseExpression();
      
      if (reporter.needMoreLines()) continue;
      if (reporter.numErrors() == 0) break;
      return 3;
    }
    
    std::cout << ": " << ast << std::endl;
  }
}

int runFile(const char* fileName)
{
  VM vm;
  vm.init();
  
  gc<String> source = readFile(fileName);
  bool success = vm.loadModule(fileName, source);
  return success ? 0 : 1;
}

int main(int argc, const char* argv[])
{
  if (argc == 1) return repl();
  if (argc == 2) return runFile(argv[1]);

  // TODO(bob): Show usage, etc.
  std::cout << "magpie <script>" << std::endl;
  return 1;
}
