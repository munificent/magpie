#include "Chunk.h"

namespace magpie {

  Chunk::Chunk(int numRegisters)
  : numRegisters_(numRegisters) {
    code_.add((123 << 16) | (0 << 8) | OP_LOAD_SHORT);
    code_.add(  (0 << 16) | (1 << 8) | OP_MOVE);
    code_.add(              (1 << 8) | OP_HACK_PRINT);
    code_.add(                         OP_RETURN);
  }

}