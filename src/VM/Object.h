#pragma once

#include <iostream>

#include "Macros.h"
#include "Managed.h"
#include "MagpieString.h"

namespace magpie
{
  class BoolObject;
  class Chunk;
  class ChannelObject;
  class ClassObject;
  class DynamicObject;
  class Fiber;
  class FunctionObject;
  class ListObject;
  class Memory;
  class Multimethod;
  class NumberObject;
  class NothingObject;
  class RecordObject;
  class StringObject;
  class Upvar;
  class VM;
  
  enum ObjectType {
    OBJECT_BOOL,
    OBJECT_CHANNEL,
    OBJECT_CLASS,
    OBJECT_DYNAMIC,
    OBJECT_FUNCTION,
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

    // Returns the object as a channel. Object *must* be a ChannelObject.
    virtual ChannelObject* asChannel()
    {
      ASSERT(false, "Not a channel.");
      return NULL;
    }
    
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

    // Returns the object as a function. Object *must* be a FunctionObject.
    virtual FunctionObject* asFunction()
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

    // Converts the object to a string. Unlike [asString], the object can be
    // any type.
    virtual gc<String> toString() const = 0;

    virtual void trace(std::ostream& stream) const;
    
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

    virtual gc<String> toString() const;
    
  private:
    bool value_;
    
    NO_COPY(BoolObject);
  };

  class ChannelObject : public Object
  {
  public:
    ChannelObject()
    : Object(),
      isOpen_(true)
    {}

    bool isOpen() const { return isOpen_; }

    // Sends the 'done' sentinel and closes the channel. Returns true if the
    // sending fiber should be suspended.
    bool close(VM& vm, gc<Fiber> sender);

    // Takes a previously sent value and returns it to [receiver]. If no value
    // has been sent yet, returns NULL.
    gc<Object> receive(VM& vm, gc<Fiber> receiver);

    // Sends a value along this channel. Returns true if a receiver was
    // available to receive it.
    bool send(VM& vm, gc<Fiber> sender, gc<Object> value);

    virtual ObjectType type() const { return OBJECT_CHANNEL; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    // Returns the object as a channel. Object *must* be a ChannelObject.
    virtual ChannelObject* asChannel() { return this; }
    
    virtual gc<String> toString() const;

    virtual void reach();

  private:
    bool isOpen_;
    
    // If a value is sent before a receiver is blocking, this in-flight value.
    gc<Object> sentValue_;

    // If a value is sent before a receiver is blocking, this refers to that
    // blocked sender.
    gc<Fiber> sender_;

    // Fibers that are currently blocked waiting to receive a value on this
    // channel.
    Array<gc<Fiber> > receivers_;

    NO_COPY(ChannelObject);
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

    virtual gc<String> toString() const;

    virtual void reach();
    
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
    static gc<DynamicObject> create(gc<ClassObject> classObj);

    // Creates a new instance of [classObj] using [args] to initialize its
    // fields.
    static gc<DynamicObject> create(ArrayView<gc<Object> >& args);
    
    virtual ObjectType type() const { return OBJECT_DYNAMIC; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual DynamicObject* asDynamic() { return this; }

    virtual gc<String> toString() const;
    
    gc<ClassObject> classObj() { return class_; }

    gc<Object> getField(int index);
    void setField(int index, gc<Object> value);

    virtual void reach();

  private:
    DynamicObject(gc<ClassObject> classObj)
    : Object(),
      class_(classObj)
    {}

    gc<ClassObject> class_;
    gc<Object>      fields_[FLEXIBLE_SIZE];

    NO_COPY(DynamicObject);
  };

  class FunctionObject : public Object
  {
  public:
    // Creates a new function with the given chunk. If there are any upvars,
    // they should be initialized after this by calling setUpvar().
    static gc<FunctionObject> create(gc<Chunk> chunk);

    virtual ObjectType type() const { return OBJECT_FUNCTION; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual FunctionObject* asFunction() { return this; }

    virtual gc<String> toString() const;

    gc<Chunk> chunk() { return chunk_; }

    gc<Upvar> getUpvar(int index);
    void setUpvar(int index, gc<Upvar> upvar);

    virtual void reach();

  private:
    FunctionObject(gc<Chunk> chunk)
    : Object(),
      chunk_(chunk)
    {}

    gc<Chunk> chunk_;
    gc<Upvar> upvars_[FLEXIBLE_SIZE];

    NO_COPY(FunctionObject);
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

    virtual gc<String> toString() const;
    
    Array<gc<Object> >& elements() { return elements_; }

    virtual void reach();

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

    virtual gc<String> toString() const;
    
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
    virtual double asNumber() const { return value_; }

    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return value_ != 0; }
    virtual gc<String> toString() const;
    
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
    virtual gc<String> toString() const;

    virtual void reach();
    
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
    virtual gc<String> toString() const { return value_; }

    virtual void reach();

  private:
    gc<String> value_;
    
    NO_COPY(StringObject);
  };
}