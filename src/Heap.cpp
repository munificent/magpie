#include "Heap.h"

#include "Managed.h"

namespace magpie {
  
  Heap::Heap(size_t size) {
    memory_ = reinterpret_cast<char*>(::operator new(size));
    free_ = memory_;
    end_ = memory_ + size;
  }
  
  Heap::~Heap() {
    operator delete(memory_);
  }
  
  bool Heap::canAllocate(size_t size) const {
    // Find the end of the allocated object.
    char* next = free_ + size;
    
    // See if it's past the end of the heap.
    return next < end_;
  }
  
  void* Heap::allocate(size_t size) {
    void* allocated = free_;
    // TODO(bob): Use C++ cast.
    char* next = free_ + size;
    
    // Make sure we don't go past the end of the heap.
    if(next >= end_) return NULL;

    free_ = next;
    return allocated;
  }

  void Heap::reset() {
    free_ = memory_;
    
    // TODO(bob): Just for debugging.
    memset(memory_, 0xbb, (unsigned char*)end_ - (unsigned char*)memory_);
  }
  
  Managed* Heap::getFirst() {
    // TODO(bob): Hackish. Should at least check that there is an object here.
    return reinterpret_cast<Managed*> (memory_);
  }
  
  Managed* Heap::getNext(Managed* current) {
    void* next = (void*)((unsigned char*)current + current->getSize());
    
    // Don't walk past the end.
    if (next >= free_) return NULL;
    return reinterpret_cast<Managed*>(next);
  }
}