#pragma once

#include <iostream>

#include "GC.h"
#include "Heap.h"
#include "Macros.h"

namespace magpie {
  class AllocScope;
  class RootSource;
  
  // The dynamic memory manager. Uses Cheney-style semi-space copying for
  // garbage collection.
  class Memory {
    friend class AllocScope;
    
  public:
    static void initialize(RootSource* roots, size_t heapSize);
    
    static void collect();
    
    static void* allocate(size_t size);
    
    template <class T>
    static temp<T> makeTemp(T* object) {
      ASSERT(currentScope_ != NULL, "Not in a scope.");
      temps_[numTemps_].set(object);
      gc<Managed>* tempSlot = &temps_[numTemps_++];
      return temp<T>(tempSlot);
    }
    
    // Indicates that the given object is reachable and should be preserved
    // during garbage collection.
    template <class T>
    static void reach(gc<T>& ref) {
      if (ref.isNull()) return;
      Managed* newLocation = copy(&(*ref));
      ref.set(static_cast<T*> (newLocation));
    }
    
  private:
    static const int MAX_TEMPS = 128; // TODO(bob): Pick less random number.
    
    // If the pointed-to object is in from-space, copies it to to-space and
    // leaves a forwarding pointer. If it's a forwarding pointer already, just
    // updates the reference. Returns the new address of the object.
    static Managed* copy(Managed* obj);

    static void pushScope(AllocScope* scope);
    static void popScope();

    static RootSource*  roots_;
    
    // Pointers to a and b. These will swap back and forth on each collection.
    static Heap* from_;
    static Heap* to_;

    // The actual heaps.
    static Heap a_;
    static Heap b_;

    static AllocScope* currentScope_;
    static gc<Managed> temps_[MAX_TEMPS];
    static int numTemps_;
  };
  
  class AllocScope {
  public:
    AllocScope()
    : previous_(NULL),
      numTempsBefore_(Memory::numTemps_) {
      Memory::pushScope(this);
    }
    
    ~AllocScope() {
      Memory::popScope();
    }  
    
  private:
    AllocScope* previous_;
    int numTempsBefore_;
    
    friend class Memory;
    
    NO_COPY(AllocScope);
    STACK_ONLY;
  };
}

