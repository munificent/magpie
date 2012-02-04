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

// Reads a file from the given path into a String.
temp<String> readFile(const char* path)
{
  std::ifstream stream(path);

  if (stream.fail())
  {
    std::cout << "Could not open file '" << path << "'." << std::endl;
    return temp<String>();
  }

  // From: http://stackoverflow.com/questions/2602013/read-whole-ascii-file-into-c-stdstring.
  std::string str;

  // Allocate a std::string big enough for the file.
  stream.seekg(0, std::ios::end);
  str.reserve(stream.tellg());
  stream.seekg(0, std::ios::beg);

  // Read it in.
  str.assign((std::istreambuf_iterator<char>(stream)),
             std::istreambuf_iterator<char>());

  return String::create(str.c_str());
}

int main(int argc, char * const argv[])
{
  std::cout << "Magpie!\n";

  // TODO(bob): Hack temp!
  VM vm;
  AllocScope scope;

  // Try lexing a file.
  /*
  temp<String> source = readFile("../../example/Fibonacci.mag");
  Lexer lexer(source);
  while (true)
  {
    AllocScope tokenScope;
    temp<Token> token = lexer.readToken();
    std::cout << token << std::endl;
    if (token->type() == TOKEN_EOF) break;
  }
  */
  
  temp<String> source = String::create("var i = 123\ni");
  //  temp<String> source = String::create("if false then 2 + 3 else 4 * 5");
  Parser parser(source);
  temp<Node> node = parser.parseExpression();
  
  std::cout << node << std::endl;
  temp<Method> method = Compiler::compileMethod(*node);

  temp<Object> result = vm.fiber().interpret(method);
  std::cout << result << std::endl;
  
  return 0;
}
