#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "GC.h"
#include "VM.h"

using namespace magpie;

int main(int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  // TODO(bob): Hack temp!
  VM vm;
  gc<Fiber> fiber = vm.getFiber();

  gc<Chunk> return3 = gc<Chunk>(new (vm) Chunk(1));
  unsigned short three = fiber->addLiteral(Object::create(vm, 3.0));
  return3->write(MAKE_LITERAL(three, 0));
  return3->write(MAKE_RETURN(0));
  
  gc<Object> return3Method = gc<Object>(new (vm) Multimethod(return3));
  unsigned short method = fiber->addLiteral(return3Method);
  
  gc<Chunk> chunk = gc<Chunk>(new (vm) Chunk(3));
  unsigned short zero = fiber->addLiteral(Object::create(vm, 0));
  chunk->write(MAKE_LITERAL(zero, 0));
  chunk->write(MAKE_LITERAL(method, 1));
  chunk->write(MAKE_CALL(0, 1, 2));
  chunk->write(MAKE_MOVE(2, 0));
  chunk->write(MAKE_HACK_PRINT(0));
  chunk->write(MAKE_RETURN(0));
  
  fiber->interpret(chunk);
  
  return 0;
}
