#pragma once

#include <iostream>

#include "Array.h"
#include "Macros.h"
#include "Semispace.h"

namespace magpie
{
  class Managed;
  class Memory;
  
  // A reference to an object on the garbage-collected heap. It's basically a
  // wrapper around a pointer, but it clarifies in code which pointers are to
  // GC objects and which aren't. This is a value type: it can be freely copied
  // and passed by value.
  template <class T>
  class gc
  {
  public:
    // Constructs a new gc pointer.
    gc(T* object)
    : object_(object)
    {}
    
    gc()
    : object_(NULL)
    {}
    
    // GC pointers can be freely copied.
    gc(const gc& object)
    : object_(object.object_)
    {}
    
    template <class S>
    gc(const gc<S>& object)
    : object_(object.object_)
    {
      CHECK_SUBTYPE(T, S);
    }
    
    gc<T>& operator =(T* right)
    {
      object_ = right;
      return *this;
    }
    
    gc<T>& operator =(const gc<T>& right)
    {
      if (&right != this) object_ = right.object_;
      return *this;
    }
    
    T& operator *() const
    {
      ASSERT(object_ != NULL, "Cannot dereference NULL pointer.");
      return *static_cast<T*>(object_);
    }
    
    T* operator ->() const
    {
      ASSERT(object_ != NULL, "Cannot dereference NULL pointer.");
      return static_cast<T*>(object_);
    }
    
    // Compare two references. If both are non-null then compares the objects.
    template <class S>
    bool operator ==(const gc<S>& other) const
    {
      CHECK_SUBTYPE(T, S);
      
      // Have to either both be null or neither.
      if (isNull() != other.isNull()) return false;
      
      const T* left = static_cast<const T*>(object_);
      const S* right = static_cast<const S*>(other.object_);
      return *left == *right;
    }
    
    // Compares two references.
    template <class S>
    inline bool operator !=(const gc<S>& other) const
    {
      return !(this == other);
    }
    
    bool isNull() const { return object_ == NULL; }
    
    // Unlike operator ==, this only checks that the two gc<T> objects are
    // referring to the exact same object in memory.
    inline bool sameAs(gc<T> other)
    {
      return object_ == other.object_;
    }
    
    void set(T* object) { object_ = object; }
    
  private:
    Managed* object_;

    // This is so that gcs with different type arguments can access each other's
    // privates. In particular, the casting copy constructor relies on this.
    template <class> friend class gc;
  };

  template <class T>
  std::ostream& operator <<(std::ostream& out, const gc<T>& object)
  {
    if (object.isNull())
    {
      out << "null";
    }
    else
    {
      out << *object;
    }
    return out;
  };

  class RootSource;

  // The dynamic memory manager. Uses a basic Cheney-style semi-space copying
  // collector. To keep things extremely simple, this GC has a couple of
  // restrictions.
  //
  //   * It will not force a garbage collection during an allocation. Instead,
  //     it relies checkCollect() being called at some convenient time before
  //     memory is needed. checkCollect() will do a collection if the amount of
  //     free space is below some threshold. As long as you never need to
  //     allocate more than that threshold between calls to checkCollect(),
  //     everything works fine.
  //
  //   * It does not trace temporaries that are on the stack. The only roots it
  //     knows about are the ones in the provided RootSource. This means that
  //     you should not call checkCollect() while there are references to GC
  //     objects on the C stack.
  //
  //   * You must be very careful about `this`. This is a copying collector, so
  //     every live object will move when a collection occurs. If you call
  //     checkCollect() while you are inside an instance method of some GC class
  //     then that object itself will be moved, invalidating the this pointer.
  //     Trying to access any instance after that will do Bad Things. To avoid
  //     this, checkCollect() should only be called when no GC object methods
  //     are on the stack.
  //
  // What can I say, it's my first GC.
  class Memory
  {
  public:
    static void initialize(RootSource* roots, size_t heapSize);
    static void shutDown();
    
    static bool checkCollect();

    static void* allocate(size_t size);

    static int numCollections() { return numCollections_; }
    
    // Indicates that the given object is reachable and should be preserved
    // during garbage collection.
    template <class T>
    static void reach(gc<T>& ref)
    {
      if (ref.isNull()) return;
      Managed* newLocation = copy(&(*ref));
      ref.set(static_cast<T*> (newLocation));
    }
    
    // Indicates that the given array of objects is reachable and should be
    // preserved during garbage collection.
    template <class T>
    static void reach(Array<gc<T> >& array)
    {
      for (int i = 0; i < array.count(); i++)
      {
        reach(array[i]);
      }
    }
    
  private:
    // If the pointed-to object is in from-space, copies it to to-space and
    // leaves a forwarding pointer. If it's a forwarding pointer already, just
    // updates the reference. Returns the new address of the object.
    static Managed* copy(Managed* obj);
    
    static RootSource*  roots_;

    // Pointers to a and b. These will swap back and forth on each collection.
    static Semispace* from_;
    static Semispace* to_;

    // The actual semispaces.
    static Semispace a_;
    static Semispace b_;

    static int numCollections_;
  };
}

