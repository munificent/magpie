#pragma once

#include "Array.h"
#include "Callframe.h"
#include "Chunk.h"
#include "Object.h"
#include "Ref.h"

namespace magpie {

  class Fiber {
  public:
    Fiber();
    
    void interpret(Ref<Chunk> chunk);
    
    // TODO(bob): Temp?
    unsigned short addLiteral(Ref<Object> value);
    
  private:
    void run();
    void call(Ref<Chunk> chunk, Ref<Object> arg);
    
    Array<Ref<CallFrame> > stack_;
    Array<Ref<Object> >    literals_;
    
    NO_COPY(Fiber);
  };

}