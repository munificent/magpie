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
  class NothingObject;
  class RecordObject;
  class StringObject;
  
  enum ObjectType {
    OBJECT_BOOL,
    OBJECT_CLASS,
    OBJECT_NOTHING,
    OBJECT_NUMBER,
    OBJECT_RECORD,
    OBJECT_STRING
  };
  
  class Object : public Managed
  {
  public:
    Object() : Managed() {}
    
    virtual ObjectType type() const = 0;
    
    virtual bool toBool() const
    {
      ASSERT(false, "Not a bool.");
      return false;
    }
    
    virtual const ClassObject* toClass() const
    {
      ASSERT(false, "Not a class.");
      return NULL;
    }
    
    virtual double toNumber() const
    {
      ASSERT(false, "Not a number.");
      return 0;
    }
    
    virtual RecordObject* toRecord() { return NULL; }
    
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
    
    virtual ObjectType type() const { return OBJECT_BOOL; }
    
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
    
    gc<String> name() const { return name_; }
    
    bool is(const ClassObject& other) const;
    
    virtual ObjectType type() const { return OBJECT_CLASS; }
    
    virtual const ClassObject* toClass() const { return this; }
    
    virtual void reach();
    
    virtual void trace(std::ostream& stream) const
    {
      stream << name_;
    }
    
  private:
    gc<String> name_;
  };
  
  class NothingObject : public Object
  {
  public:
    NothingObject()
    : Object()
    {}
    
    virtual ObjectType type() const { return OBJECT_NOTHING; }
    
    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return false; }
    
    virtual void trace(std::ostream& stream) const
    {
      stream << "nothing";
    }
    
  private:
    NO_COPY(NothingObject);
  };
  
  class NumberObject : public Object
  {
  public:
    NumberObject(double value)
    : Object(),
      value_(value)
    {}
    
    virtual ObjectType type() const { return OBJECT_NUMBER; }
    
    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return value_ != 0; }
    virtual double toNumber() const { return value_; }
    
    virtual void trace(std::ostream& stream) const
    {
      stream << value_;
    }
    
  private:
    double value_;
    
    NO_COPY(NumberObject);
  };
  
  // A record's "type" is an implicit class that describes the set of fields
  // that a record has.
  class RecordType : public Managed
  {
  public:
    static gc<RecordType> create(const Array<int>& fields);
    
    int numFields() const { return numFields_; }
    
    // Returns the index for the given field, or -1 if this record doesn't have
    // a field with that name. Given a RecordObject whose RecordType is this,
    // the index of a field here will be the index in that object's fields for
    // the field with the given name.
    int getField(symbolId symbol) const;
    
    // Given the index of a field in this type, returns the symbol ID of that
    // field.
    symbolId getSymbol(int index) const;
    
  private:
    RecordType(const Array<int>& fields);
    
    int numFields_;
    int names_[FLEXIBLE_SIZE];
  };
  
  // A record or tuple object.
  class RecordObject : public Object
  {
  public:
    static gc<Object> create(gc<RecordType> type,
                             const Array<gc<Object> >& stack, int startIndex);
    
    gc<Object> getField(int symbol);
    
    virtual ObjectType type() const { return OBJECT_RECORD; }

    virtual bool toBool() const { return true; }
    virtual RecordObject* toRecord() { return this; }
    
    virtual void reach();
    virtual void trace(std::ostream& stream) const;
    
  private:
    RecordObject(gc<RecordType> type)
    : Object(),
      type_(type)
    {}
    
    gc<RecordType> type_;
    gc<Object>     fields_[FLEXIBLE_SIZE];
    
    NO_COPY(RecordObject);
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
    
    virtual ObjectType type() const { return OBJECT_STRING; }
    
    virtual gc<String> toString() const { return value_; }
    
    virtual void reach();
    
    virtual void trace(std::ostream& stream) const
    {
      stream << value_;
    }
    
  private:
    gc<String> value_;
    
    NO_COPY(StringObject);
  };
}