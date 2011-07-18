#pragma once

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
    
  private:
    NO_COPY(Object);
  };

}