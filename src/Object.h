#pragma once

#include <iostream>

#include "Macros.h"
#include "Ref.h"

namespace magpie {
  class Multimethod;
  class NumberObject;
  
  class Object {
  public:
    static Ref<Object> create(double value);
    
    Object() {}
    
    virtual Multimethod*  asMultimethod() { return NULL; }
    virtual NumberObject* asNumber()      { return NULL; }
    
    // TODO(bob): Debug only.
    virtual void debugTrace(std::ostream & stream) const = 0;

  private:
    NO_COPY(Object);
  };
  
  // TODO(bob): Debug only.
  std::ostream & operator<<(std::ostream & cout, const Object & object);
  
}