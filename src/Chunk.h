#pragma once

#include "Array.h"

#define GET_OP(opcode) ((unsigned char)(opcode) & 0xff)

#define OP_MOVE       (0x01) //  unused(8) | from(8) | to(8) | opcode(8)
#define OP_LOAD_SHORT (0x02) //  value(16) | register(8) | opcode(8)
#define OP_CALL       (0x03) //  result register(8) | arg register(8) | method register(8) | opcode(8)
#define OP_RETURN     (0x04) // unused(16) | register(8) | opcode(8)
#define OP_HACK_PRINT (0x05) // unused(16) | register(8) | opcode(8)

namespace magpie {

  class Chunk {
  public:
    Chunk(int numRegisters);
    
    // Gets the instruction at the given index. Indexes are zero-based from the
    // beginning of the array. Negative indexes are from the end of the array
    // and go forward, so that -1 is the last item in the array.
    const unsigned int operator[] (int index) const {
      return code_[index];
    }
    
    int getNumRegisters() const { return numRegisters_; }
    
  private:
    int numRegisters_;
    Array<unsigned int> code_;
    
    NO_COPY(Chunk);
  };

}