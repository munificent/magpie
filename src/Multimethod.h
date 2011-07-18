#pragma once

#include "Chunk.h"
#include "Macros.h"
#include "Object.h"
#include "Ref.h"

namespace magpie {
  
  class Multimethod : public Object {
  public:
    Multimethod() {}
    
    virtual Multimethod* asMultimethod() { return this; }

    // Selects an appropriate method for the given argument and returns its
    // code.
    Ref<Chunk> select(Ref<Object> arg);
    
  private:
    NO_COPY(Multimethod);
  };
  
}