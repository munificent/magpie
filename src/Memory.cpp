#include "Memory.h"

#include "Fiber.h"
#include "ForwardingAddress.h"
#include "GC.h"
#include "Managed.h"
#include "VM.h"

namespace magpie {
  
  Memory::Memory(VM& vm, size_t heapSize)
  : vm_(vm),
    a_(heapSize),
    b_(heapSize) {
    to_ = &a_;
    from_ = &b_;
  }
  
  void Memory::collect() {
    std::cout << "Collecting...\n";
    
    // Copy the root to to-space.
    gc<Fiber>& root = vm_.getFiber();
    copy(root);
    
    // Walk through to-space, copying over every object reachable from it.
    Managed* reached = to_->getFirst();
    while (reached != NULL) {
      reached->reachRefs(*this);
      reached = to_->getNext(reached);
    }

    // Swap the semi-spaces. Everything is now in to_ which becomes the new
    // from_ for the next collection.
    Heap* temp = from_;
    from_ = to_;
    to_ = temp;
  }
  
  void* Memory::allocate(size_t size) {
    if (!from_->canAllocate(size)) {
      // Heap is full, so trigger a GC.
      collect();
    }
    
    // TODO(bob): Handle failure.
    return from_->allocate(size);
  }
  
  template <class T>
  void Memory::copy(gc<T>& ref) {
    // See if what we're pointing to has already been moved.
    T* forward = static_cast<T*>(ref->getForwardingAddress());
    if (forward) {
      // It has, so just update this reference.
      ref.set(forward);
    } else {
      // It hasn't, so copy it to to-space.
      size_t size = ref->getSize();
      T* dest = reinterpret_cast<T*>(to_->allocate(size));
      memcpy(dest, &(*ref), size);
      
      // Replace the old object with a forwarding address.
      ::new (ref.getRawPointer()) ForwardingAddress(dest);
      
      // Update the reference to point to the new location.
      ref.set(dest);
    }
  }

}