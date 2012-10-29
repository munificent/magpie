#pragma once

#include <iostream>

#include "Macros.h"
#include "Managed.h"
#include "MagpieString.h"

namespace magpie
{
  class BoolObject;
  class ClassObject;
  class DynamicObject;
  class ListObject;
  class Memory;
  class Multimethod;
  class NumberObject;
  class NothingObject;
  class RecordObject;
  class StringObject;
  class VM;
  
  enum ObjectType {
    OBJECT_BOOL,
    OBJECT_CLASS,
    OBJECT_DYNAMIC,
    OBJECT_LIST,
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

    // Gets the ClassObject for this object's class.
    virtual gc<ClassObject> getClass(VM& vm) const = 0;
    
    // Returns the object as a class. Object *must* be a ClassObject.
    virtual ClassObject* asClass()
    {
      ASSERT(false, "Not a class.");
      return NULL;
    }
    
    // Returns the object as a dynamic object. Object *must* be a DynamicObject.
    virtual DynamicObject* asDynamic()
    {
      ASSERT(false, "Not a dynamic object.");
      return NULL;
    }
    
    // Returns the object as a list. Object *must* be a ListObject.
    virtual ListObject* asList()
    {
      ASSERT(false, "Not a list.");
      return NULL;
    }

    // Returns the object as a number. Object *must* be a NumberObject.
    virtual double asNumber() const
    {
      ASSERT(false, "Not a number.");
      return 0;
    }
    
    // Returns the object as a string. Object *must* be a StringObject.
    virtual gc<String> asString() const
    {
      ASSERT(false, "Not a string.");
      return gc<String>();
    }

    // Returns the boolean value of the object.
    virtual bool toBool() const { return true; }

    // Returns the object as a RecordObject if it is one, otherwise `NULL`.
    virtual RecordObject* toRecord() { return NULL; }
    
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

    virtual gc<ClassObject> getClass(VM& vm) const;

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
    ClassObject(gc<String> name, int numFields)
    : Object(),
      name_(name),
      numFields_(numFields)
    {}
    
    gc<String> name() const { return name_; }
    int numFields() const { return numFields_; }
    
    bool is(const ClassObject& other) const;
    
    virtual ObjectType type() const { return OBJECT_CLASS; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual ClassObject* asClass() { return this; }
    
    virtual void reach();
    
    virtual void trace(std::ostream& stream) const
    {
      stream << name_;
    }
    
  private:
    gc<String> name_;
    int numFields_;
  };
  
  // A regular instance of some class.
  class DynamicObject : public Object
  {
  public:
    // Creates a new instance of [classObj]. This should only be used for
    // built-in classes which do not have any fields.
    static gc<Object> create(gc<ClassObject> classObj);

    // Creates a new instance of [classObj] using [args] to initialize its
    // fields.
    static gc<Object> create(ArrayView<gc<Object> >& args);
    
    virtual ObjectType type() const { return OBJECT_DYNAMIC; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual DynamicObject* asDynamic() { return this; }

    virtual void trace(std::ostream& stream) const
    {
      stream << "[instance of " << class_->name() << "]";
    }
    
    gc<ClassObject> classObj() { return class_; }

    gc<Object> getField(int index);
    void setField(int index, gc<Object> value);

  private:
    DynamicObject(gc<ClassObject> classObj)
    : Object(),
      class_(classObj)
    {}

    gc<ClassObject> class_;
    gc<Object>      fields_[FLEXIBLE_SIZE];

    NO_COPY(DynamicObject);
  };
  
  class ListObject : public Object
  {
  public:
    ListObject(int capacity)
    : Object(),
      elements_(capacity)
    {}
    
    virtual ObjectType type() const { return OBJECT_LIST; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual ListObject* asList() { return this; }
    
    virtual void trace(std::ostream& stream) const;
    
    Array<gc<Object> >& elements() { return elements_; }
    
  private:
    Array<gc<Object> > elements_;
    
    NO_COPY(ListObject);
  };
  
  class NothingObject : public Object
  {
  public:
    NothingObject()
    : Object()
    {}
    
    virtual ObjectType type() const { return OBJECT_NOTHING; }

    virtual gc<ClassObject> getClass(VM& vm) const;

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

    virtual gc<ClassObject> getClass(VM& vm) const;

    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return value_ != 0; }
    virtual double asNumber() const { return value_; }
    
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

    NO_COPY(RecordType);
  };
  
  // A record or tuple object.
  class RecordObject : public Object
  {
  public:
    static gc<Object> create(gc<RecordType> type,
                             const Array<gc<Object> >& stack, int startIndex);
    
    gc<Object> getField(int symbol);
    
    virtual ObjectType type() const { return OBJECT_RECORD; }

    virtual gc<ClassObject> getClass(VM& vm) const;

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

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> asString() const { return value_; }
    
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