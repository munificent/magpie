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
    friend class AllocScope;
    
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
    static const int MAX_TEMPS = 128; // TODO(bob): Pick less random number.
    
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

    gc<Managed> temps_[MAX_TEMPS];
    int numTemps_;
    
    NO_COPY(Memory);
  };
  
  class AllocScope {
  public:
    AllocScope(Memory & memory)
    : memory_(memory),
      numTempsBefore_(memory_.numTemps_) {}
    
    ~AllocScope() {
      memory_.numTemps_ = numTempsBefore_;
    }
    
    Memory & memory() { return memory_; }
    
    template <class T>
    temp<T> makeTemp(T* object) {
      memory_.temps_[memory_.numTemps_].set(object);
      gc<Managed>* tempSlot = &memory_.temps_[memory_.numTemps_++];
      return temp<T>(tempSlot);
    }    
    
  private:
    Memory & memory_;
    int numTempsBefore_;
    
    STACK_ONLY(AllocScope);
  };
}

