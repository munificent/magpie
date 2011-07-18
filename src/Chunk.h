#pragma once

#include "Array.h"
#include "Bytecode.h"

namespace magpie {

  class Chunk {
  public:
    Chunk(int numRegisters);
    
    // Gets the instruction at the given index. Indexes are zero-based from the
    // beginning of the array. Negative indexes are from the end of the array
    // and go forward, so that -1 is the last item in the array.
    const bytecode operator[] (int index) const {
      return code_[index];
    }
    
    int getNumRegisters() const { return numRegisters_; }
    
    // TODO(bob): Temp?
    void write(bytecode instruction) {
      code_.add(instruction);
    }
    
  private:
    int numRegisters_;
    Array<bytecode> code_;
    
    NO_COPY(Chunk);
  };

}