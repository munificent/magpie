#pragma once

#include <iostream>

#include "Common.h"
#include "Data/String.h"
#include "Memory/Managed.h"
#include "Syntax/Ast.h"

namespace magpie
{
  class AtomObject;
  class Chunk;
  class ChannelObject;
  class CharacterObject;
  class ClassObject;
  class DynamicObject;
  class Fiber;
  class FloatObject;
  class FunctionObject;
  class IntObject;
  class ListObject;
  class Memory;
  class Multimethod;
  class NothingObject;
  class Object;
  class RecordObject;
  class StringObject;
  class Upvar;
  class VM;

  // Unsafe downcasting functions. These must *only* be called after the object
  // has been verified as being the right type.
  gc<ChannelObject> asChannel(gc<Object> obj);
  unsigned int asCharacter(gc<Object> obj);
  gc<ClassObject> asClass(gc<Object> obj);
  gc<DynamicObject> asDynamic(gc<Object> obj);
  double asFloat(gc<Object> obj);
  gc<FunctionObject> asFunction(gc<Object> obj);
  int asInt(gc<Object> obj);
  gc<ListObject> asList(gc<Object> obj);
  gc<String> asString(gc<Object> obj);

  class Object : public Managed
  {
  public:
    Object() : Managed() {}

    // Gets the ClassObject for this object's class.
    virtual gc<ClassObject> getClass(VM& vm) const = 0;

    // Returns the boolean value of the object.
    virtual bool toBool() const { return true; }

    // Returns the object as a RecordObject if it is one, otherwise `NULL`.
    virtual RecordObject* toRecord() { return NULL; }

    // Converts the object to a string. Unlike [asString], the object can be
    // any type.
    virtual gc<String> toString() const = 0;

    // Returns true if this object is equivalent to [other]. This is "built-in"
    // equality and does not take into account user-defined specializations of
    // the "==" operator.
    virtual bool equals(gc<Object> other) { return this == &*other; }

    // Double-dispatch methods for value equality.
    virtual bool equalsChar(unsigned int value) { return false; }
    virtual bool equalsFloat(double value) { return false; }
    virtual bool equalsInt(int value) { return false; }
    virtual bool equalsString(gc<String> value) { return false; }

    virtual void trace(std::ostream& stream) const;
  };

  // An atomic value like true or nothing.
  class AtomObject : public Object
  {
  public:
    AtomObject(Atom atom)
    : Object(),
      atom_(atom)
    {}

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual bool toBool() const;

    virtual gc<String> toString() const;

  private:
    Atom atom_;
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

    // Sends a value along this channel.
    void send(gc<Fiber> sender, gc<Object> value);

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> toString() const;

    virtual void reach();

  private:
    bool isOpen_;

    // TODO(bob): Make these a linked-list queue.
    // The fibers that are suspended waiting to send a value on this channel.
    Array<gc<Fiber> > senders_;

    // The fibers that are suspended waiting to receive a value on this channel.
    Array<gc<Fiber> > receivers_;
  };

  class CharacterObject : public Object
  {
  public:
    CharacterObject(unsigned int value)
    : Object(),
      value_(value)
    {}

    unsigned int value() const { return value_; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return value_ != 0; }
    virtual gc<String> toString() const;

    virtual bool equals(gc<Object> other) { return other->equalsChar(value_); }
    virtual bool equalsChar(unsigned int value) { return value == value_; }

  private:
    unsigned int value_;
  };

  class ClassObject : public Object
  {
  public:
    static gc<ClassObject> create(gc<String> name, int numFields,
                                  int numSuperclasses,
                                  const ArrayView<gc<Object> >& superclasses);

    gc<String> name() const { return name_; }
    int numFields() const { return numFields_; }

    bool is(const ClassObject& other) const;

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> toString() const;

    virtual void reach();

  private:
    ClassObject(gc<String> name, int numFields, int numSuperclasses)
    : Object(),
      name_(name),
      numFields_(numFields),
      numSuperclasses_(numSuperclasses)
    {}

    gc<String>      name_;
    int             numFields_;
    int             numSuperclasses_;
    gc<ClassObject> superclasses_[FLEXIBLE_SIZE];
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

    virtual gc<ClassObject> getClass(VM& vm) const;

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
  };

  class FloatObject : public Object
  {
  public:
    FloatObject(double value)
    : Object(),
    value_(value)
    {}

    double value() const { return value_; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return value_ != 0; }
    virtual gc<String> toString() const;

    virtual bool equals(gc<Object> other) { return other->equalsFloat(value_); }
    virtual bool equalsFloat(double value) { return value == value_; }

  private:
    double value_;
  };

  class FunctionObject : public Object
  {
  public:
    // Creates a new function with the given chunk. If there are any upvars,
    // they should be initialized after this by calling setUpvar().
    static gc<FunctionObject> create(gc<Chunk> chunk);

    virtual gc<ClassObject> getClass(VM& vm) const;

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
  };

  class IntObject : public Object
  {
  public:
    IntObject(int value)
    : Object(),
      value_(value)
    {}

    int value() const { return value_; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    // TODO(bob): Do we want to do this here, or rely on a "true?" method?
    virtual bool toBool() const { return value_ != 0; }
    virtual gc<String> toString() const;

    virtual bool equals(gc<Object> other) { return other->equalsInt(value_); }
    virtual bool equalsInt(int value) { return value == value_; }

  private:
    // TODO(bob): long?
    int value_;
  };

  class ListObject : public Object
  {
  public:
    ListObject(int capacity)
    : Object(),
      elements_(capacity)
    {}

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> toString() const;

    Array<gc<Object> >& elements() { return elements_; }

    virtual void reach();

  private:
    Array<gc<Object> > elements_;
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

    gc<String> value() const { return value_; }

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> toString() const { return value_; }

    virtual bool equals(gc<Object> other) { return other->equalsString(value_); }
    virtual bool equalsString(gc<String> value) { return value == value_; }

    virtual void reach();

  private:
    gc<String> value_;
  };
}
