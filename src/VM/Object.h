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

    virtual double toNumber()
    {
      ASSERT(false, "Not a number.");
      return 0;
    }

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
    
    virtual double toNumber() { return value_; }
    
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