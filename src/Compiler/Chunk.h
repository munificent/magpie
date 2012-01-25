#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Managed.h"

namespace magpie
{

  class Chunk : public Managed
  {
  public:
    Chunk(int numRegisters);

    // Gets the instruction at the given index. Indexes are zero-based from the
    // beginning of the array. Negative indexes are from the end of the array
    // and go forward, so that -1 is the last item in the array.
    const instruction operator[] (int index) const
    {
      return code_[index];
    }

    int getNumRegisters() const { return numRegisters_; }

    // TODO(bob): Temp?
    void write(instruction i)
    {
      code_.add(i);
    }

  private:
    int numRegisters_;
    Array<instruction> code_;

    NO_COPY(Chunk);
  };

}