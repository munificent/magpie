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
  /*
  AllocScope* Memory::currentScope_ = NULL;
  gc<Managed> Memory::temps_[Memory::MAX_TEMPS];
  int Memory::numTemps_ = 0;
   */
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
    /*
    numTemps_ = 0;
     */
    numCollections_ = 0;
  }
  
  void Memory::shutDown()
  {
    ASSERT(roots_ != NULL, "Not initialized.");
    
    roots_ = NULL;
    a_.shutDown();
    b_.shutDown();
  }

  void Memory::collect()
  {
    // TODO(bob): There is a significant bug here. When objects are moved,
    // every temp<T> on the stack is fixed. Every gc<T> stored in an object is
    // fixed. However, there's (at least) one big missing piece: this. The
    // callframes on the C stack are typically methods. Those methods have an
    // implicit "this" pointer. That isn't being updated. So if you do:
    //
    // struct Foo
    // {
    //   int field;
    //   void method()
    //   {
    //     Object::create(); // allocate and cause a GC
    //     cout << field; // access a field on this
    //   }
    // };
    //
    // Then bad things happen because you're accessing memory in the dead
    // semispace, not the current one.
    
    /*
    // Copy the active temps to to-space.
    for (int i = 0; i < numTemps_; i++)
    {
      reach(temps_[i]);
    }
    */
    
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
  }
  
  void* Memory::allocate(size_t size)
  {
    if (!from_->canAllocate(size))
    {
      std::cout << "Oh, dear. Out of memory." << std::endl;
      exit(-1);
      /*
      // Heap is full, so trigger a GC.
      collect();
     */
    }
    
    // TODO(bob): Handle failure.
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
      
      Managed* dest = static_cast<Managed*>(to_->allocate(size));
      memcpy(dest, obj, size);
      
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

  /*
  void Memory::pushScope(AllocScope* scope)
  {
    ASSERT_NOT_NULL(scope);
    scope->previous_ = currentScope_;
    currentScope_ = scope;
  }
  
  void Memory::popScope()
  {
    ASSERT_NOT_NULL(currentScope_);

    // Clear out the popped temps to help track down GC bugs.
    for (int i = currentScope_->numTempsBefore_; i < numTemps_; i++)
    {
      temps_[i] = gc<Managed>();
    }

    numTemps_ = currentScope_->numTempsBefore_;
    currentScope_ = currentScope_->previous_;
  }
   */
}