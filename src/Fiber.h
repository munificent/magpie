#pragma once

#include "Array.h"
#include "Callframe.h"
#include "Chunk.h"
#include "Object.h"
#include "Ref.h"

namespace magpie {

  class Fiber {
  public:
    Fiber() {}
    
    void interpret(Ref<Chunk> chunk);
    
  private:
    void run();
    void call(Ref<Chunk> chunk, Ref<Object> arg);
    
    Array<Ref<CallFrame> > stack_;
    Ref<Object>            return_;
    
    NO_COPY(Fiber);
  };

}