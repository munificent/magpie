#pragma once

#include "Array.h"
#include "Chunk.h"
#include "Macros.h"
#include "Multimethod.h"
#include "Object.h"
#include "Ref.h"

namespace magpie {
  class CallFrame {
  public:
    CallFrame(Ref<Chunk> chunk);
    
    int  getInstruction()                { return instruction_; }
    void setInstruction(int instruction) { instruction_ = instruction; }
    
    Ref<Chunk>       getChunk()           { return chunk_; }
    Ref<Object>      getRegister(int index) { return registers_[index]; }
    
    void setRegister(int index, Ref<Object> value) { registers_[index] = value; }
    
  private:
    Ref<Chunk>               chunk_;
    int                      instruction_;
    Array<Ref<Object> >      registers_;
    
    NO_COPY(CallFrame);
  };
}

