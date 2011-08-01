#pragma once

#include <iostream>

#include "GC.h"
#include "Heap.h"
#include "Macros.h"

namespace magpie {
  class Managed;
  class VM;
  
  // The dynamic memory manager. Uses Cheney-style semi-space copying for
  // garbage collection.
  class Memory {
  public:
    typedef float(*reachCallback)(Memory*, Managed*);
    
    Memory(VM& vm, size_t heapSize);
    
    void collect();
    
    void* allocate(size_t size);

    // If the pointed-to object is in from-space, copies it to to-space and
    // leaves a forwarding pointer. If it's a forwarding pointer already, just
    // updates the reference. After calling, ref will point to the new object
    // in to-space.
    template <class T>
    void copy(gc<T>& ref);
    
  private:
    
    VM&  vm_;
    
    // Pointers to a and b. These will swap back and forth on each collection.
    Heap* from_;
    Heap* to_;

    // The actual heaps.
    Heap a_;
    Heap b_;

    NO_COPY(Memory);
  };
}

