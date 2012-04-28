#pragma once

#include "Macros.h"

namespace magpie
{
  class Managed;

  // A contiguous chunk of garbage collected memory. For each object, it stores
  // its size followed by the object's memory. Objects are allocated
  // sequentially, so allocation is little more than a pointer increment. It
  // does not support deallocating individual objects.
  class Semispace
  {
  public:
    Semispace();
    ~Semispace();

    void initialize(size_t size);
    void shutDown();

    // Gets whether or not an object of the given size can be allocated in this
    // heap.
    bool canAllocate(size_t size) const;

    // Try to allocate a block of the given size from this heap. Returns 0 if
    // the heap doesn't have enough free space.
    void* allocate(size_t size);

    // Resets the heap back to being completely unallocated. Note that this
    // will not call destructors on any objects living on this heap. They just
    // disappear in a puff of smoke.
    void reset();

    // Gets the first object contained in this heap.
    Managed* getFirst();

    // Gets the next object in the heap following the given one, or NULL if the
    // given object is the last one in the heap.
    Managed* getNext(Managed* current);

    inline size_t size() const { return end_ - memory_; }
    inline size_t amountAllocated() const { return free_ - memory_; }
    inline size_t amountFree() const { return end_ - free_; }
    
  private:
    char* memory_;
    char* free_;  // The first byte of available memory.
    char* end_;   // The first byte past the end of the heap.

    NO_COPY(Semispace);
  };
}

