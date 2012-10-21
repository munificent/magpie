#include "Memory.h"

#include "Fiber.h"
#include "ForwardingAddress.h"
#include "Managed.h"
#include "RootSource.h"

namespace magpie
{  
  RootSource* Memory::roots_ = NULL;
  Semispace Memory::a_;
  Semispace Memory::b_;
  int Memory::numCollections_ = 0;
  Semispace* Memory::to_ = NULL;
  Semispace* Memory::from_ = NULL;
  
  void Memory::initialize(RootSource* roots, size_t heapSize)
  {
    ASSERT_NOT_NULL(roots);
    ASSERT(roots_ == NULL, "Already initialized.");
    
    roots_ = roots;
    a_.initialize(heapSize);
    b_.initialize(heapSize);
    to_ = &a_;
    from_ = &b_;
    numCollections_ = 0;
  }
  
  void Memory::shutDown()
  {
    ASSERT(roots_ != NULL, "Not initialized.");
    
    roots_ = NULL;
    a_.shutDown();
    b_.shutDown();
  }

  bool Memory::checkCollect()
  {
    // Don't collect if we've got room.
    // TODO(bob): Tune this. It basically needs to be the maximum amount of
    // memory we could want to allocate between calls to checkCollect().
    if (from_->amountFree() > 1024) return false;
    
    size_t freeBefore = from_->amountFree();
    
    // Copy the roots to to-space.
    roots_->reachRoots();
    
    // Walk through to-space, copying over every object reachable from it.
    Managed* reached = to_->getFirst();
    while (reached != NULL)
    {
      reached->reach();
      reached = to_->getNext(reached);
    }

    // We've copied everything reachable from from_ so it can be cleared now.
    from_->reset();
    
    // Swap the semi-spaces. Everything is now in to_ which becomes the new
    // from_ for the next collection.
    Semispace* temp = from_;
    from_ = to_;
    to_ = temp;
    
    numCollections_++;
    
    if (freeBefore >= from_->amountFree())
    {
      // TODO(bob): Do something more graceful here.
      std::cout << "Out of memory. Only " << from_->amountFree()
                << " bytes available after garbage collection." << std::endl;
      exit(-1);
    }
    
    /*
    std::cout << "GC collect " << numCollections_
              << ", free before " << freeBefore
              << ", after " << from_->amountFree()
              << ", reclaimed " << (from_->amountFree() - freeBefore)
              << "." << std::endl;
    */
    return true;
  }
  
  void* Memory::allocate(size_t size)
  {
    if (!from_->canAllocate(size))
    {
      // TODO(bob): Do something better here. We don't trigger a GC here right
      // now because we want to ensure that GC (which involves moving objects)
      // only happens at well-defined points where we know there aren't any
      // references to GC objects on the stack. Right now, we don't support
      // tracking temporaries like that, so instead we just call checkCollect()
      // at a point in time when we know there aren't any to worry about.
      std::cout << "Out of memory. Need " << size << " and only "
                << from_->amountFree() << " available." << std::endl;
      exit(-1);
    }
    
    return from_->allocate(size);
  }
  
  Managed* Memory::copy(Managed* obj)
  {
    // See if what we're pointing to has already been moved.
    Managed* forward = obj->getForwardingAddress();
    if (forward)
    {
      // It has, so just update this reference.
      return forward;
    }
    else
    {
      // It hasn't, so copy it to to-space.
      
      // The size is stored directly before the object.
      size_t size = *reinterpret_cast<size_t*>(
          reinterpret_cast<char*>(obj) - sizeof(size_t));

      void* mem = to_->allocate(size);
      memcpy(mem, static_cast<void*>(obj), size);

      Managed* dest = static_cast<Managed*>(mem);

      // Clear it out so we can track down GC bugs.
      /*
      memset(obj, 0xcc, size);
      */
      
      // Replace the old object with a forwarding address.
      ::new (obj) ForwardingAddress(dest);
      
      // Update the reference to point to the new location.
      return static_cast<Managed*>(dest);
    }
  }
}