#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "Ref.h"

using namespace magpie;

int main (int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  // TODO(bob): Hack temp!
  Fiber fiber;
  unsigned short literal = fiber.addLiteral(Ref<Object>(Object::create(1234.56)));

  Ref<Chunk> chunk = Ref<Chunk>(new Chunk(2));
  chunk->write(MAKE_LITERAL(literal, 0));
  chunk->write(MAKE_MOVE(0, 1));
  chunk->write(MAKE_HACK_PRINT(1));
  chunk->write(MAKE_RETURN(0));
  
  fiber.interpret(chunk);
  
  return 0;
}
