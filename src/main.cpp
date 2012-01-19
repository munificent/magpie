#include <fstream>
#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "GC.h"
#include "VM.h"
#include "MagpieString.h"

using namespace magpie;

struct Cons : public magpie::Managed {
  Cons(int id) : id(id) {}
  
  virtual size_t allocSize() const { return sizeof(Cons); }
  virtual void reach(Memory& memory) {
    memory.reach(next);
  }
  
  int id;
  gc<Cons> next;
};

struct TestRoots : public magpie::RootSource {
  virtual void reachRoots(Memory& memory) {
    memory.reach(root);
  }
  
  gc<Cons> root;
};

void testCollector() {
  TestRoots roots;
  Memory memory(roots, 10000000);
  AllocScope scope(memory);
  
  gc<Cons> notRoot;
  
  // Make two long cons chains, only one of which is rooted.
  gc<Cons>* a = &roots.root;
  gc<Cons>* b = &notRoot;
  int id = 0;
  for (int i = 0; i < 600000; i++) {
    a->set(new (scope) Cons(id++));
    a = &((*a)->next);

    b->set(new (scope) Cons(id++));
    b = &((*b)->next);
  }
}

// Reads a file from the given path into a String.
temp<String> readFile(AllocScope& scope, const char* path) {
  std::ifstream stream(path);
  
  if (stream.fail()) {
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
  
  return String::create(scope, str.c_str());
}

int main(int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  testCollector();
  
  // TODO(bob): Hack temp!
  VM vm;
  AllocScope scope(vm.memory());
  gc<Fiber>& fiber = vm.fiber();

  // Try reading a file.
  temp<String> source = readFile(scope, "../../example/Calculator.mag");
  std::cout << source->cString() << std::endl;
  
  gc<Chunk> return3 = gc<Chunk>(new (scope) Chunk(1));
  unsigned short three = fiber->addLiteral(Object::create(scope, 3.0));
  return3->write(MAKE_LITERAL(three, 0));
  return3->write(MAKE_RETURN(0));
  
  gc<Object> return3Method = gc<Object>(new (scope) Multimethod(return3));
  unsigned short method = fiber->addLiteral(return3Method);
  
  gc<Chunk> chunk = gc<Chunk>(new (scope) Chunk(3));
  unsigned short zero = fiber->addLiteral(Object::create(scope, 0));
  chunk->write(MAKE_LITERAL(zero, 0));
  chunk->write(MAKE_LITERAL(method, 1));
  chunk->write(MAKE_CALL(0, 1, 2));
  chunk->write(MAKE_MOVE(2, 0));
  chunk->write(MAKE_HACK_PRINT(0));
  chunk->write(MAKE_RETURN(0));
  
  fiber->interpret(chunk);
  
  return 0;
}
