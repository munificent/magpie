#pragma once

#include <iostream>

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class BoolObject;
  class Memory;
  class Multimethod;
  class NumberObject;

  class Object : public Managed
  {
  public:
    static temp<BoolObject> create(bool value);
    static temp<NumberObject> create(double value);

    Object() : Managed() {}
    
    virtual bool toBool() const
    {
      ASSERT(false, "Not a bool.");
      return false;
    }
    
    virtual double toNumber() const
    {
      ASSERT(false, "Not a number.");
      return 0;
    }

  private:
    NO_COPY(Object);
  };
  
  class BoolObject : public Object
  {
  public:
    BoolObject(bool value)
    : Object(),
      value_(value)
    {}
    
    virtual bool toBool() const { return value_; }
    
    virtual void trace(std::ostream& stream) const
    {
      stream << (value_ ? "true" : "false");
    }
    
  private:
    bool value_;
    
    NO_COPY(BoolObject);
  };
  
  class NumberObject : public Object
  {
  public:
    NumberObject(double value)
    : Object(),
      value_(value)
    {}
    
    virtual double toNumber() const { return value_; }
    
    virtual void trace(std::ostream& stream) const
    {
      stream << value_;
    }
    
  private:
    double value_;
    
    NO_COPY(NumberObject);
  };
}