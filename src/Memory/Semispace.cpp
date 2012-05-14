#include "Semispace.h"

#include "ForwardingAddress.h"
#include "Managed.h"

namespace magpie
{
  Semispace::Semispace()
  : memory_(NULL),
    free_(NULL),
    end_(NULL) {}

  Semispace::~Semispace()
  {
    if (memory_ != NULL) operator delete(memory_);
  }

  void Semispace::initialize(size_t size)
  {
    ASSERT(memory_ == NULL, "Already initialized.");

    memory_ = reinterpret_cast<char*>(::operator new(size));
    free_ = memory_;
    end_ = memory_ + size;

    // Store a zero for the size of the "first" object so that we can tell if
    // the heap is empty.
    *reinterpret_cast<size_t*>(memory_) = 0;
  }

  void Semispace::shutDown()
  {
    ASSERT(memory_ != NULL, "Not initialized.");
    
    ::operator delete(memory_);
    memory_ = NULL;
    free_ = NULL;
    end_ = NULL;
  }
  
  bool Semispace::canAllocate(size_t size) const
  {
    // Find the end of the allocated object.
    char* next = free_ + size + sizeof(size_t);

    // See if it's past the end of the heap.
    return next < end_;
  }

  void* Semispace::allocate(size_t size)
  {
    // When this object is copied, it will be replaced with a forwarding
    // address. We need to ensure we always have enough room for that too.
    if (size < sizeof(ForwardingAddress))
    {
      size = sizeof(ForwardingAddress);
    }
    
    // The allocated object will start just after its size.
    char* allocated = free_ + sizeof(size_t);
    char* next = allocated + size;

    // Make sure we don't go past the end of the heap.
    if (next >= end_) return NULL;

    free_ = next;
    
    // Store the allocated size so we know where the next object starts.
    *reinterpret_cast<size_t*>(allocated - sizeof(size_t)) = size;
    
    return allocated;
  }

  void Semispace::reset()
  {
    free_ = memory_;
    
    // Clear it out so we can track down GC bugs.
    /*
    memset(memory_, 0xff, end_ - memory_);
    */
  }

  Managed* Semispace::getFirst()
  {
    // Bail if there are no objects in the heap.
    if (*reinterpret_cast<size_t*>(memory_) == 0) return NULL;
    
    // Skip past the size.
    return reinterpret_cast<Managed*>(memory_ + sizeof(size_t));
  }

  Managed* Semispace::getNext(Managed* current)
  {
    // Get the size of the current object so we know how far to skip.
    char* pos = reinterpret_cast<char*>(current);
    size_t size = *reinterpret_cast<size_t*>(pos - sizeof(size_t));
    char* next = pos + size;
    
    // Don't walk past the end.
    if (next >= free_) return NULL;
    
    // Skip past the size header of the next object.
    return reinterpret_cast<Managed*>(next + sizeof(size_t));
  }
}