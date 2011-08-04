#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "GC.h"
#include "VM.h"

using namespace magpie;

struct Cons : public magpie::Managed {
  Cons(int id) : id(id) {}
  
  virtual size_t getSize() const { return sizeof(Cons); }
  virtual void reach(Memory& memory) {
    std::cout << "reach " << id << "\n";
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
  Memory memory(roots, 1000);
  
  gc<Cons> notRoot;
  
  // Make two long cons chains, only one of which is rooted.
  gc<Cons>* a = &roots.root;
  gc<Cons>* b = &notRoot;
  int id = 0;
  for (int i = 0; i < 600; i++) {
    std::cout << "allocate " << id << "\n";
    a->set(new (memory) Cons(id++));
    a = &((*a)->next);

    std::cout << "allocate " << id << "\n";
    b->set(new (memory) Cons(id++));
    b = &((*b)->next);
  }
}

int main(int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  testCollector();
  
  // TODO(bob): Hack temp!
  VM vm;
  gc<Fiber> fiber = vm.getFiber();

  gc<Chunk> return3 = gc<Chunk>(new (vm.getMemory()) Chunk(1));
  unsigned short three = fiber->addLiteral(Object::create(vm.getMemory(), 3.0));
  return3->write(MAKE_LITERAL(three, 0));
  return3->write(MAKE_RETURN(0));
  
  gc<Object> return3Method = gc<Object>(new (vm.getMemory()) Multimethod(return3));
  unsigned short method = fiber->addLiteral(return3Method);
  
  gc<Chunk> chunk = gc<Chunk>(new (vm.getMemory()) Chunk(3));
  unsigned short zero = fiber->addLiteral(Object::create(vm.getMemory(), 0));
  chunk->write(MAKE_LITERAL(zero, 0));
  chunk->write(MAKE_LITERAL(method, 1));
  chunk->write(MAKE_CALL(0, 1, 2));
  chunk->write(MAKE_MOVE(2, 0));
  chunk->write(MAKE_HACK_PRINT(0));
  chunk->write(MAKE_RETURN(0));
  
  fiber->interpret(chunk);
  
  return 0;
}
