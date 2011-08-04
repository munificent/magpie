#pragma once

#include <iostream>

#include "GC.h"
#include "Heap.h"
#include "Macros.h"

namespace magpie {
  class RootSource;
  
  // The dynamic memory manager. Uses Cheney-style semi-space copying for
  // garbage collection.
  class Memory {
  public:
    Memory(RootSource& roots, size_t heapSize);
    
    void collect();
    
    void* allocate(size_t size);
    
    // Indicates that the given object is reachable and should be preserved
    // during garbage collection.
    template <class T>
    void reach(gc<T>& ref) {
      if (ref.isNull()) return;
      Managed* newLocation = copy(&(*ref));
      ref.set(static_cast<T*> (newLocation));
    }
    
  private:
    
    // If the pointed-to object is in from-space, copies it to to-space and
    // leaves a forwarding pointer. If it's a forwarding pointer already, just
    // updates the reference. Returns the new address of the object.
    Managed* copy(Managed* obj);

    RootSource&  roots_;
    
    // Pointers to a and b. These will swap back and forth on each collection.
    Heap* from_;
    Heap* to_;

    // The actual heaps.
    Heap a_;
    Heap b_;
    
    int numAllocated_;
    int numCopied_;

    NO_COPY(Memory);
  };
}

