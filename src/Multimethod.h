#pragma once

#include "Chunk.h"
#include "Macros.h"
#include "Object.h"
#include "Ref.h"

namespace magpie {
  
  class Multimethod : public Object {
  public:
    Multimethod(Ref<Chunk> code)
    : code_(code) {}
    
    virtual Multimethod* asMultimethod() { return this; }
    
    virtual void debugTrace(std::ostream & stream) const {
      stream << "(multimethod)";
    }
    
    // Selects an appropriate method for the given argument and returns its
    // code.
    Ref<Chunk> select(Ref<Object> arg);
    
  private:
    // TODO(bob): Temp!
    Ref<Chunk> code_;
    
    NO_COPY(Multimethod);
  };
  
}