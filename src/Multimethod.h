#pragma once

#include "Chunk.h"
#include "GC.h"
#include "Macros.h"
#include "Object.h"

namespace magpie {
  
  class Multimethod : public Object {
  public:
    Multimethod(gc<Chunk> code)
    : code_(code) {}
    
    virtual size_t getSize() const { return sizeof(Multimethod); }

    virtual Multimethod* asMultimethod() { return this; }
    
    virtual void debugTrace(std::ostream & stream) const {
      stream << "(multimethod)";
    }
    
    // Selects an appropriate method for the given argument and returns its
    // code.
    gc<Chunk> select(gc<Object> arg);
    
  private:
    // TODO(bob): Temp!
    gc<Chunk> code_;
    
    NO_COPY(Multimethod);
  };
  
}