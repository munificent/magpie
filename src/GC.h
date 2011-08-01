#pragma once

#include <iostream>

#include "Macros.h"

namespace magpie {
  
  // A pointer to a garbage-collected object. T should be a class that inherits
  // Managed.
  template <class T>
  class gc {
  public:
    // Constructs a new gc pointer.
    explicit gc(T* obj)
    : obj_(obj) {}
    
    gc()
    : obj_(NULL) {}
    
    // GC pointers can be freely copied.
    gc(const gc<T>& obj)
    : obj_(obj.obj_) {}
    
    T& operator *() const { return *obj_; }
    T* operator ->() const { return obj_; }
    
    void* getRawPointer() const {
      return reinterpret_cast<void*>(obj_);
    }
    
    void set(T* obj) {
      obj_ = obj;
    }
    
  private:
    T* obj_;
  };
  
  template <class T>
  std::ostream& operator<<(std::ostream& out, const gc<T>& obj) {
    out << *obj;
    return out;
  };
  
}

