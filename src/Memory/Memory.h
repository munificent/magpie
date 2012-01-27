#pragma once

#include <iostream>

#include "Macros.h"
#include "Semispace.h"

namespace magpie
{
  class Managed;
  class Memory;
  
  // Base class for gc. Used to resolve a circular dependency between temp and
  // gc. Shouldn't be used directly.
  class GcBase
  {
  public:
    bool isNull() const { return object_ == NULL; }
    
  private:
    GcBase(Managed* object)
    : object_(object) {}
    
    Managed* object_;
    
    template<class> friend class temp;
    template<class> friend class gc;
  };
  
  // A reference to an object in the managed heap that isn't reachable from a
  // root. This class ensures that newly created objects won't get collected
  // before they have a chance to get tied to the reachability graph. This
  // class should only be used on the stack. As its name implies, you can
  // maintain long-lived instances of this class.
  //
  // New objects that live on the GC heap will be created within a AllocScope
  // and will be returned through an instance of this class. As long as the
  // AllocScope object is still in scope, any temps created using it will be
  // valid and won't be accidentally garbage collected.
  //
  // This class is lightweight and can be freely passed around by value and
  // copied. Internally, it uses two levels of indirection to refer to the
  // wrapped object (i.e. it's a pointer to a pointer). This ensures that it
  // can maintain a reference even if the underlying object is moved by the
  // GC.
  template <class T>
  class temp
  {    
  public:
    // Creates a null temp reference.
    temp()
    : object_(NULL) {}
    
    template <class S>
    temp(const temp<S>& right)
    : object_(right.object_)
    {
      CHECK_SUBTYPE(T, S);
    }
    
    bool isNull() const
    {
      return object_ == NULL || object_->object_ == NULL;
    }
    
    // Gets the underlying object being referred to.
    T& operator *() const { return *static_cast<T*>(object_->object_); }
    T* operator ->() const { return static_cast<T*>(object_->object_); }
    
    // Compare two temps. If both are non-null then compares the objects.
    template <class S>
    bool operator ==(const temp<S>& right) const
    {
      CHECK_SUBTYPE(T, S);
      
      // Have to either both be null or neither.
      if (isNull() != right.isNull()) return false;
      return *object_ == *right.object_;
    }
    
    template <class S>
    inline bool operator !=(const temp<S>& right) const
    {
      return !(this == right);
    }
    
  private:
    temp(GcBase* object)
    : object_(object)
    {}
    
    GcBase* object_;
    
    friend class Memory;
    template<class> friend class gc;
    // This is so that temps with different type arguments can access each
    // other's privates.
    template<class> friend class temp;
  };
  
  template <class T>
  std::ostream& operator <<(std::ostream& out, const temp<T>& object)
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
  
  // A rooted reference to an object in the managed heap. Use this type to hold
  // reachable references to GC objects. Mainly, this means that fields of
  // managed objects that refer to other managed objects will generally use
  // this type.
  template <class T>
  class gc : public GcBase
  {
  public:
    // Constructs a new gc pointer.
    explicit gc(T* object)
    : GcBase(object)
    {}
    
    gc()
    : GcBase(NULL)
    {}
    
    // GC pointers can be freely copied.
    gc(const gc& object)
    : GcBase(object.object_)
    {}
    
    // You can promote a temp to a gc.
    template <class S>
    gc(const temp<S>& object)
    : GcBase(object.isNull() ? NULL : object.object_->object_)
    {
      CHECK_SUBTYPE(T, S);
    }
    
    gc<T>& operator =(const gc<T>& right)
    {
      if (&right != this) object_ = right.object_;
      return *this;
    }
    
    gc<T>& operator =(const temp<T>& right)
    {
      object_ = right.object_->object_;
      return *this;
    }
    
    T& operator *() const { return *static_cast<T*>(object_); }
    T* operator ->() const { return static_cast<T*>(object_); }
    
    // Compare two gc pointers. If both are non-null then compares the objects.
    template <class S>
    bool operator ==(const gc<S>& other) const
    {
      CHECK_SUBTYPE(T, S);
      
      // Have to either both be null or neither.
      if (isNull() != other.isNull()) return false;
      
      return *object_ == *other.object_;
    }
    
    // Compares two references. References are not equal if they refer to
    // different objects.
    template <class S>
    inline bool operator !=(const gc<S>& other) const
    {
      return !(this == other);
    }
    
    temp<T> toTemp() const;
    
    void set(T* object)
    {
      object_ = object;
    }
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

  class AllocScope;
  class RootSource;

  // The dynamic memory manager. Uses Cheney-style semi-space copying for
  // garbage collection.
  class Memory
  {
    friend class AllocScope;

  public:
    static void initialize(RootSource* roots, size_t heapSize);
    static void shutDown();
    
    static void collect();

    static void* allocate(size_t size);

    static int numCollections() { return numCollections_; }
    
    template <class T>
    static temp<T> makeTemp(T* object)
    {
      ASSERT(currentScope_ != NULL, "Not in a scope.");
      ASSERT(numTemps_ < MAX_TEMPS, "Too many temps.");

      temps_[numTemps_].set(object);
      gc<Managed>* tempSlot = &temps_[numTemps_++];
      return temp<T>(tempSlot);
    }

    // Indicates that the given object is reachable and should be preserved
    // during garbage collection.
    template <class T>
    static void reach(gc<T>& ref)
    {
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
    static Semispace* from_;
    static Semispace* to_;

    // The actual semispaces.
    static Semispace a_;
    static Semispace b_;

    static AllocScope* currentScope_;
    static gc<Managed> temps_[MAX_TEMPS];
    static int numTemps_;
    static int numCollections_;
  };

  class AllocScope
  {
  public:
    AllocScope()
    : previous_(NULL),
      numTempsBefore_(Memory::numTemps_)
    {
      Memory::pushScope(this);
    }

    ~AllocScope()
    {
      if (numTempsBefore_ != CLOSED) Memory::popScope();
    }

    // Closes this scope. Any temps created after calling this will be created
    // in the enclosing scope (in any).
    //
    // More importantly, this lifts a temp out of this scope and stores it in
    // the enclosing one. This way, you can create an object and return it
    // safely while escaping the local AllocScope. It enables this pattern:
    //
    //     temp<Foo> makeFoo()
    //     {
    //       AllocScope scope;
    //
    //       temp<Bar> bar = Bar::create();
    //       temp<Baz> baz = Baz::create();
    //       temp<Bang> bang = Bang::create();
    //       temp<Foo> foo = Foo::create(bar, baz, bang);
    //       return scope.close(foo);
    //     }
    //
    // Here, the memory manager can now reuse the temporary slots that were
    // used for `bar`, `baz` and `bang` by closing the scope while still
    // ensuring `foo` is kept alive.
    template <class T>
    temp<T> close(temp<T> object)
    {
      ASSERT(numTempsBefore_ != CLOSED, "Cannot close a scope more than once.");
      
      // Snag the object.
      T* rawObject = &(*object);
      
      // Close this scope.
      Memory::popScope();
      numTempsBefore_ = CLOSED;
      
      // Now place it in a temp in the enclosing scope.
      return Memory::makeTemp(rawObject);
    }
    
  private:
    static const int CLOSED = -1;
    
    AllocScope* previous_;
    int numTempsBefore_;

    friend class Memory;

    NO_COPY(AllocScope);
    STACK_ONLY;
  };
  
  template <class T>
  temp<T> gc<T>::toTemp() const
  {
    return Memory::makeTemp(static_cast<T*>(object_));
  }
}

