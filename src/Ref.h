#pragma once

#include <iostream>

#include "Macros.h"

namespace magpie {

  // Referenced-linked smart pointer.
  template <class T>
  class Ref {
  public:
    // Constructs a new null pointer.
    Ref()
    : obj_(NULL),
      prev_(this),
      next_(this) {}
    
    // Wraps the given raw pointer in a new smart pointer. This should only
    // be used once for any given pointer (i.e. you wrap the raw pointer as
    // soon as its allocated and then only access it through Ref<T> from
    // that point on.
    explicit Ref(T * obj)
    : obj_(obj),
      prev_(this),
      next_(this) {}
    
    // Copies a reference. Both references will refer to the same object.
    Ref(const Ref<T> & other)
    : obj_(NULL),
      prev_(this),
      next_(this) {
      if (&other != this) link(other);
    }
    
    ~Ref() { clear(); }
    
    T & operator *() const { return *obj_; }
    T * operator ->() const { return obj_; }
    
    // Compares two references. References are equal if they refer to the
    // same object.
    bool operator ==(const Ref<T> & other) const {
      return obj_ == other.obj_;
    }
    
    // Compares two references. References are not equal if they refer to
    // different objects.
    bool operator !=(const Ref<T> & other) const {
      return obj_ != other.obj_;
    }
    
    // Discards the currently referred to object and assigns the given
    // reference to this one.
    Ref<T>& operator =(const Ref<T> & other) {
      if (&other != this) {
        clear();
        link(other);
      }
      
      return *this;
    }
    
    // Gets whether or not this reference is pointing to null.
    bool IsNull() const { return obj_ == NULL; }
    
    // Clears the reference. If this was the last reference to the referred
    // object, it will be deallocated.
    void clear() {
      if (next_ != this) {
        // unlink it
        prev_->next_ = next_;
        next_->prev_ = prev_;
        
        prev_ = this;
        next_ = this;
      } else if (obj_ != NULL) {
        // linked to itself, so it's the last reference
        delete obj_;
      }
      
      obj_ = NULL;
    }
    
  private:
    
    void link(const Ref<T> & other) {
      // don't bother to share null
      if (other.obj_ != NULL) {
        obj_ = other.obj_;
        
        // link it in
        prev_ = other.prev_;
        next_ = &other;
        
        other.prev_ = this;
        prev_->next_ = this;
      }
    }
    
    T * obj_;
    
    mutable const Ref<T> * prev_;
    mutable const Ref<T> * next_;
  };

  template <class T>
  std::ostream& operator<<(std::ostream& cout, const Ref<T> & ref) {
    if (ref.IsNull()) {
      cout << "(null reference)";
    } else {
      cout << *ref;
    }
    
    return cout;
  }

}

