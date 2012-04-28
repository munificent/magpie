#include <fstream>
#include <iostream>

#include "Compiler.h"
#include "Fiber.h"
#include "MagpieString.h"
#include "Node.h"
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

int main(int argc, char * const argv[])
{
  // TODO(bob): Hack temp!
  VM vm;
  
  // Read a file.
  const char* fileName = "../../example/Fibonacci2.mag"; // "../../script/big.mag";
  ErrorReporter reporter;
  gc<String> source = readFile(fileName);
  Parser parser(fileName, source, reporter);
  gc<ModuleAst> module = parser.parseModule();
  
  // Compile it.
  Compiler::compileModule(vm, module, reporter);
  
  // Invoke main().
  if (reporter.numErrors() == 0)
  {
    gc<Object> result = vm.run();
    cout << result << endl;
  }
  
  return 0;
}
