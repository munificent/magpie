#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "GC.h"
#include "VM.h"

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

int main(int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  testCollector();
  
  // TODO(bob): Hack temp!
  VM vm;
  AllocScope scope(vm.getMemory());
  gc<Fiber> fiber = vm.getFiber();

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
