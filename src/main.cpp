#include <iostream>

#include "Chunk.h"
#include "Fiber.h"
#include "Ref.h"

using namespace magpie;

int main (int argc, char * const argv[]) {
  std::cout << "Magpie!\n";
  
  Fiber fiber;
  fiber.interpret(Ref<Chunk>(new Chunk(2)));
  
  return 0;
}
