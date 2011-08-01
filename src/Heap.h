#pragma once

#include "Macros.h"

namespace magpie {
  class Managed;
  
  // TODO(bob): rename semispace
  // A contiguous chunk of garbage collected memory.
  class Heap {
  public:
    Heap(size_t size);
    ~Heap();
    
    // Gets whether or not an object of the given size can be allocated in this
    // heap.
    bool canAllocate(size_t size) const;
    
    // Try to allocate a block of the given size from this heap. Returns 0 if
    // the heap doesn't have enough free space.
    void* allocate(size_t size);
    
    // Gets the first object contained in this heap.
    Managed* getFirst();
    
    // Gets the next object in the heap following the given one, or NULL if the
    // given object is the last one in the heap.
    Managed* getNext(Managed* current);
    
  private:
    void* memory_;
    void* free_;  // The first byte of available memory.
    void* end_;   // The first byte past the end of the heap.
    
    NO_COPY(Heap);
  };
}

