#pragma once

#include "Array.h"

#define OP_MOVE       (0x01) // A: from, B: to
#define OP_LOAD_SHORT (0x02) // AB: value, C: register
#define OP_CALL       (0x03) // A: arg, B: method, C: result
#define OP_RETURN     (0x04) // A: result
#define OP_HACK_PRINT (0x05) // A: register

#define MAKE_MOVE(from, to)       ((from << 24) | (to << 16) | OP_MOVE)
#define MAKE_LOAD_SHORT(value, r) ((value << 16) | (r << 8) | OP_LOAD_SHORT)
#define MAKE_CALL(arg, method, result) ((arg << 24) | (method << 16) | (result << 8) | OP_CALL)
#define MAKE_RETURN(result) ((result << 24) | OP_RETURN)
#define MAKE_HACK_PRINT(r) ((r << 24) | OP_HACK_PRINT)

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