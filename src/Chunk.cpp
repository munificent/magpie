#include "Chunk.h"

namespace magpie {

  Chunk::Chunk(int numRegisters)
  : numRegisters_(numRegisters) {
    code_.add(MAKE_LOAD_SHORT(123, 0));
    code_.add(MAKE_MOVE(0, 1));
    code_.add(MAKE_HACK_PRINT(1));
    code_.add(MAKE_RETURN(0));
  }

}