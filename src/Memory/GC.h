#pragma once

#include <iostream>

#include "Macros.h"

namespace magpie {
  class Managed;
  
  // Base class for gc. Used to resolve a circular dependency between temp and
  // gc. Shouldn't be used directly.
  class GcBase {
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
  class temp {
  public:
    // TODO(bob): Should have to go through AllocScope.
    temp(GcBase* object)
    : object_(object) {}
    
    // Gets the underlying object being referred to.
    T& operator *() { return *static_cast<T*>(object_->object_); }
    T* operator ->() { return static_cast<T*>(object_->object_); }
    
  private:
    GcBase* object_;

    template<class> friend class gc;
  };
  
  // A rooted reference to an object in the managed heap. Use this type to hold
  // reachable references to GC objects. Mainly, this means that fields of
  // managed objects that refer to other managed objects will generally use
  // this type.
  template <class T>
  class gc : public GcBase {
  public:
    // Constructs a new gc pointer.
    explicit gc(T* object)
    : GcBase(object) {}
    
    gc()
    : GcBase(NULL) {}
    
    // GC pointers can be freely copied.
    gc(const gc& object)
    : GcBase(object.object_) {}
    
    // You can promote a temp to a gc.
    gc(const temp<T>& object)
    : GcBase(object.object_->object_) {}
    
    gc<T>& operator =(const gc<T>& other)
    {
      if (&other != this) object_ = other.object_;
      return *this;
    }
    
    gc<T>& operator =(const temp<T>& other)
    {
      object_ = other.object_->object_;
      return *this;
    }
    
    T& operator *() const { return *static_cast<T*>(object_); }
    T* operator ->() const { return static_cast<T*>(object_); }
    
    void set(T* object) {
      object_ = object;
    }
  };
  
  template <class T>
  std::ostream& operator<<(std::ostream& out, const gc<T>& object) {
    out << *object;
    return out;
  };
}

