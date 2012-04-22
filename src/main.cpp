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
temp<String> readFile(const char* path)
{
  ifstream stream(path);

  if (stream.fail())
  {
    cout << "Could not open file '" << path << "'." << endl;
    return temp<String>();
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

// TODO(bob): Move this to its own file.
// Reads a pre-compiled bytecode module (i.e. a .magc file). The format is:
//
//     u32 - 'magc' magic number
//     u16 - number of methods
//     for each method:
//         u16   - number of bytes in method name
//         [u8]  - method name
//         u32   - number of instructions
//         [u32] - instructions
//         u8    - number of registers
//         u8    - number of constants
//         for each constant:
//             u8 - type (0: number, 1: string)
//             if type == 0:
//                 double - value
//             if type == 1:
//                 u32  - number of bytes in string
//                 [u8] - string
class ModuleLoader
{
public:
  static void load(VM& vm, const char* path)
  {
    ModuleLoader loader(vm, path);
    loader.readFile();
  }
  
private:
  ModuleLoader(VM& vm, const char* path)
  : vm_(vm),
    file_()
  {
    file_.open(path, ios::in | ios::binary);
    // TODO(bob): Handle errors.
  }
  
  ~ModuleLoader()
  {
    file_.close();
  }
  
  void readFile()
  {
    char magic[4];
    file_.read(magic, 4);
    // TODO(bob): Verify that it's 'magc'.
    cout << magic[0] << magic[1] << magic[2] << magic[3] << endl;
    
    int numMethods = readU16();
    cout << "num methods: " << numMethods << endl;
    for (int i = 0; i < numMethods; i++)
    {
      readMethod();
    }
  }
  
  void readMethod()
  {
    temp<String> name = readString();
    
    int numInstructions = readU16();
    Array<instruction> code;
    
    for (int i = 0; i < numInstructions; i++)
    {
      code.add(readU32());
    }
    
    int numRegisters = readU8();
    int numConstants = readU8();
    
    Array<gc<Object> > constants;
    // TODO(bob): String constants.
    for (int i = 0; i < numConstants; i++)
    {
      constants.add(NumberObject::create(readDouble()));
    }
    
    temp<Method> method = Method::create(name, code, constants, numRegisters);
    // TODO(bob): Should add in one step.
    vm_.globals().declare(name);
    vm_.globals().define(name, method);
  }
  
  temp<String> readString()
  {
    int length = readU16();
    // TODO(bob): Lame way to do this. Should allocate the gc string first and
    // then copy directly to it.
    char buffer[256];
    ASSERT(length < 256, "String too big for hacked buffer size.");
    
    file_.read(buffer, length);
    buffer[length] = '\0';
    
    return String::create(buffer, length);
  }
  
  double readDouble()
  {
    double value;
    // TODO(bob): Assumes file and machine have same endianness.
    file_.read(reinterpret_cast<char*>(&value), 4);
    
    return value;
  }
  
  unsigned char readU8()
  {
    unsigned char value;
    file_.read(reinterpret_cast<char*>(&value), 1);
    return value;
  }
  
  unsigned short readU16()
  {
    unsigned short value;
    // TODO(bob): Assumes file and machine have same endianness.
    file_.read(reinterpret_cast<char*>(&value), 2);
    
    return value;
  }
  
  unsigned int readU32()
  {
    unsigned int value;
    // TODO(bob): Assumes file and machine have same endianness.
    file_.read(reinterpret_cast<char*>(&value), 4);
    
    return value;
  }
  
  VM&      vm_;
  ifstream file_;
};

int main(int argc, char * const argv[])
{
  cout << "Magpie!\n";

  // TODO(bob): Hack temp!
  VM vm;
  AllocScope scope;

  // Read a bytecode file.
  //  ModuleLoader::load(vm, "../../example/out.magc");
  
  // Read a file.
  temp<String> source = readFile("../../example/Fibonacci2.mag");
  Parser parser(source);
  temp<ModuleAst> module = parser.parseModule();
  
  // Compile it.
  Compiler::compileModule(vm, module);
  
  // Invoke main().
  gc<Method> method = vm.globals().findMain();
  temp<Object> result = vm.fiber().interpret(method);
  cout << result << endl;
  
  return 0;
}
