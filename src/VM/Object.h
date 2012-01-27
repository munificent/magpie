#pragma once

#include <iostream>

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Memory;
  class Multimethod;
  class NumberObject;

  class Object : public Managed
  {
  public:
    static temp<NumberObject> create(double value);

    Object() : Managed() {}

    virtual NumberObject* asNumber()      { return NULL; }

  private:
    NO_COPY(Object);
  };
  
  class NumberObject : public Object
  {
  public:
    NumberObject(double value)
    : Object(),
      value_(value)
    {}
    
    virtual NumberObject* asNumber() { return this; }
    
    virtual void trace(std::ostream& stream) const
    {
      stream << value_;
    }
    
    double getValue() const { return value_; }
    
  private:
    double value_;
    
    NO_COPY(NumberObject);
  };
}