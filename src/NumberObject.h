#pragma once

#include "Macros.h"
#include "Object.h"

namespace magpie {
  
  class NumberObject : public Object {
  public:
    NumberObject(double value)
    : Object(),
      value_(value) {}
    
    virtual NumberObject* asNumber() { return this; }
    
    double getValue() const { return value_; }
    
  private:
    double value_;
    
    NO_COPY(NumberObject);
  };
}