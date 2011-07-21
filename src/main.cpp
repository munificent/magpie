#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "Ref.h"

using namespace magpie;

int main(int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  // TODO(bob): Hack temp!
  Fiber fiber;

  Ref<Chunk> return3 = Ref<Chunk>(new Chunk(1));
  unsigned short three = fiber.addLiteral(Ref<Object>(Object::create(3.0)));
  return3->write(MAKE_LITERAL(three, 0));
  return3->write(MAKE_RETURN(0));
  
  Ref<Object> return3Method = Ref<Object>(new Multimethod(return3));
  unsigned short method = fiber.addLiteral(return3Method);
  
  Ref<Chunk> chunk = Ref<Chunk>(new Chunk(3));
  unsigned short zero = fiber.addLiteral(Ref<Object>(Object::create(0)));
  chunk->write(MAKE_LITERAL(zero, 0));
  chunk->write(MAKE_LITERAL(method, 1));
  chunk->write(MAKE_CALL(0, 1, 2));
  chunk->write(MAKE_MOVE(2, 0));
  chunk->write(MAKE_HACK_PRINT(0));
  chunk->write(MAKE_RETURN(0));
  
  fiber.interpret(chunk);
  
  return 0;
}
