#pragma once

#include <iostream>

#include "Macros.h"
#include "Managed.h"
#include "MagpieString.h"

namespace magpie
{
  class BoolObject;
  class ClassObject;
  class Memory;
  class Multimethod;
  class NumberObject;
  class StringObject;
  
  class Object : public Managed
  {
  public:
    static temp<BoolObject> create(bool value);
    static temp<ClassObject> createClass(gc<String> value);
    static temp<NumberObject> create(double value);
    static temp<StringObject> create(gc<String> value);

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
    
    virtual gc<String> toString() const
    {
      ASSERT(false, "Not a string.");
      return gc<String>();
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
  
  class ClassObject : public Object
  {
  public:
    ClassObject(gc<String> name)
    : Object(),
      name_(name)
    {}
    
    virtual void trace(std::ostream& stream) const
    {
      stream << name_;
    }
    
  private:
    gc<String> name_;
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
  
  // TODO(bob): The double boxing here where this has a pointer to a String is
  // lame. Consider unifying this with the real string class.
  class StringObject : public Object
  {
  public:
    StringObject(gc<String> value)
    : Object(),
      value_(value)
    {}
    
    virtual gc<String> toString() const { return value_; }
    
    virtual void trace(std::ostream& stream) const
    {
      stream << value_;
    }
    
  private:
    gc<String> value_;
    
    NO_COPY(StringObject);
  };
}