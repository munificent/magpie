#include <sstream>

#include "Object.h"
#include "VM.h"

namespace magpie
{
  using std::ostream;

  void Object::trace(std::ostream& stream) const
  {
    stream << toString();
  }
  
  gc<ClassObject> BoolObject::getClass(VM& vm) const
  {
    return vm.boolClass();
  }

  gc<String> BoolObject::toString() const
  {
    // TODO(bob): Store these as constants.
    return String::create(value_ ? "true" : "false");
  }

  bool ChannelObject::close(VM& vm, gc<Fiber> sender)
  {
    if (!isOpen_) return false;
    isOpen_ = false;

    // If no one is listening, close immediately.
    if (receivers_.count() == 0) return false;

    // Otherwise send 'done' to the receiver.
    // TODO(bob): What if there are multiple receivers?
    send(vm, sender, vm.getBuiltIn(BUILT_IN_DONE));
    return true;
  }

  gc<Object> ChannelObject::receive(VM& vm, gc<Fiber> receiver)
  {
    // If the channel is closed, immediately receive 'done'.
    if (!isOpen_)
    {
      return vm.getBuiltIn(BUILT_IN_DONE);
    }

    if (sentValue_.isNull())
    {
      // There isn't a value already available, so suspend the receiver.
      receivers_.add(receiver);
      return NULL;
    }

    // A value is already available, so receive it immediately.
    gc<Object> value = sentValue_;
    sentValue_ = NULL;

    // Now the sender can stop blocking.
    vm.addFiber(sender_);
    sender_ = NULL;

    return value;
  }

  bool ChannelObject::send(VM& vm, gc<Fiber> sender, gc<Object> value)
  {
    // If there are no blocking receivers, just wait.
    if (receivers_.count() == 0)
    {
      // TODO(bob): Can this occur?
      ASSERT(sentValue_.isNull(), "Can't back up sends.");
      
      sentValue_ = value;
      sender_ = sender;
      return false;
    }

    // Pick a receiver to wake up and receive the value.
    gc<Fiber> fiber = receivers_.removeAt(0);
    fiber->storeReturn(value);
    vm.addFiber(fiber);

    // Add the sender back to the scheduler too since it isn't blocked.
    vm.addFiber(sender);

    return true;
  }

  gc<ClassObject> ChannelObject::getClass(VM& vm) const
  {
    return vm.channelClass();
  }

  gc<String> ChannelObject::toString() const
  {
    // TODO(bob): Do something more useful here?
    return String::create("[channel]");
  }

  void ChannelObject::reach()
  {
    receivers_.reach();
    sentValue_.reach();
    sender_.reach();
  }
  
  gc<ClassObject> ClassObject::getClass(VM& vm) const
  {
    return vm.classClass();
  }

  gc<String> ClassObject::toString() const
  {
    return name_;
  }

  void ClassObject::reach()
  {
    name_.reach();
  }

  bool ClassObject::is(const ClassObject& other) const
  {
    // TODO(bob): Subtyping.
    return this == &other;
  }

  gc<DynamicObject> DynamicObject::create(gc<ClassObject> classObj)
  {
    ASSERT(classObj->numFields() == 0, "Class cannot have fields.");
    
    // Allocate enough memory for the object.
    void* mem = Memory::allocate(sizeof(DynamicObject));

    // Construct it by calling global placement new.
    return ::new(mem) DynamicObject(classObj);
  }

  gc<DynamicObject> DynamicObject::create(ArrayView<gc<Object> >& args)
  {
    gc<ClassObject> classObj = args[0]->asClass();
    
    // Allocate enough memory for the object and its fields.
    void* mem = Memory::allocate(sizeof(DynamicObject) +
        sizeof(gc<Object>) * (classObj->numFields() - 1));

    // Construct it by calling global placement new.
    gc<DynamicObject> object = ::new(mem) DynamicObject(classObj);

    // Initialize the fields.
    for (int i = 0; i < classObj->numFields(); i++)
    {
      // +1 because the first arg is the class.
      object->fields_[i] = args[i + 1];
    }

    return object;
  }

  gc<ClassObject> DynamicObject::getClass(VM& vm) const
  {
    return class_;
  }

  gc<String> DynamicObject::toString() const
  {
    return String::format("[instance of %s]", class_->name()->cString());
  }
  
  gc<Object> DynamicObject::getField(int index)
  {
    ASSERT_INDEX(index, class_->numFields());
    return fields_[index];
  }

  void DynamicObject::setField(int index, gc<Object> value)
  {
    ASSERT_INDEX(index, class_->numFields());
    fields_[index] = value;
  }

  void DynamicObject::reach()
  {
    class_.reach();

    for (int i = 0; i < class_->numFields(); i++)
    {
      fields_[i].reach();
    }
  }

  gc<FunctionObject> FunctionObject::create(gc<Chunk> chunk)
  {
    // Allocate enough memory for the object and its upvars.
    void* mem = Memory::allocate(sizeof(FunctionObject) +
                                 sizeof(gc<Upvar>) * (chunk->numUpvars() - 1));

    // Construct it by calling global placement new.
    return ::new(mem) FunctionObject(chunk);
  }
  
  gc<ClassObject> FunctionObject::getClass(VM& vm) const
  {
    return vm.functionClass();
  }

  gc<String> FunctionObject::toString() const
  {
    // TODO(bob): Do something better here.
    return String::create("[fn]");
  }

  gc<Upvar> FunctionObject::getUpvar(int index)
  {
    ASSERT_INDEX(index, chunk_->numUpvars());
    return upvars_[index];
  }

  void FunctionObject::setUpvar(int index, gc<Upvar> upvar)
  {
    ASSERT_INDEX(index, chunk_->numUpvars());
    upvars_[index] = upvar;
  }

  void FunctionObject::reach()
  {
    chunk_.reach();

    for (int i = 0; i < chunk_->numUpvars(); i++)
    {
      upvars_[i].reach();
    }
  }

  gc<ClassObject> ListObject::getClass(VM& vm) const
  {
    return vm.listClass();
  }

  gc<String> ListObject::toString() const
  {
    std::stringstream stream;

    stream << "[";
    for (int i = 0; i < elements_.count(); i++)
    {
      stream << elements_[i];
      if (i < elements_.count() - 1) stream << ", ";
    }
    stream << "]";

    return String::create(stream.str().c_str());
  }

  void ListObject::reach()
  {
    elements_.reach();
  }
  
  gc<ClassObject> NothingObject::getClass(VM& vm) const
  {
    return vm.nothingClass();
  }

  gc<String> NothingObject::toString() const
  {
    // TODO(bob): Store in constant.
    return String::create("nothing");
  }
  
  gc<ClassObject> NumberObject::getClass(VM& vm) const
  {
    return vm.numberClass();
  }

  gc<String> NumberObject::toString() const
  {
    return String::format("%g", value_);
  }

  gc<RecordType> RecordType::create(const Array<int>& fields)
  {
    // Allocate enough memory for the record and its fields.
    void* mem = Memory::allocate(sizeof(RecordType) + 
                                 sizeof(int) * (fields.count() - 1));
    
    // Construct it by calling global placement new.
    return ::new(mem) RecordType(fields);
  }

  RecordType::RecordType(const Array<int>& fields)
  : numFields_(fields.count())
  {    
    // Initialize the fields.
    for (int i = 0; i < fields.count(); i++)
    {
      names_[i] = fields[i];
    }
  }

  int RecordType::getField(symbolId symbol) const
  {
    // TODO(bob): This loop is less than ideal since it is hit every time we
    // pull a field out of a record. It will be complex, but consider a more
    // advanced solution to this.
    for (int i = 0; i < numFields_; i++)
    {
      if (names_[i] == symbol) return i;
    }
    
    return -1;
  }
  
  symbolId RecordType::getSymbol(int index) const
  {
    ASSERT_INDEX(index, numFields_);
    return names_[index];
  }

  gc<Object> RecordObject::create(gc<RecordType> type,
      const Array<gc<Object> >& stack, int startIndex)
  {
    // Allocate enough memory for the record and its fields.
    void* mem = Memory::allocate(sizeof(RecordObject) + 
                                 sizeof(gc<Object>) * (type->numFields() - 1));
    
    // Construct it by calling global placement new.
    gc<RecordObject> record = ::new(mem) RecordObject(type);
    
    // Initialize the fields.
    for (int i = 0; i < type->numFields(); i++)
    {
      record->fields_[i] = stack[startIndex + i];
    }
    
    return record;
  }

  gc<Object> RecordObject::getField(int symbol)
  {
    int index = type_->getField(symbol);
    if (index == -1) return gc<Object>();
    
    return fields_[index];
  }

  gc<ClassObject> RecordObject::getClass(VM& vm) const
  {
    return vm.recordClass();
  }

  gc<String> RecordObject::toString() const
  {
    std::stringstream stream;

    // TODO(bob): Handle named fields.
    stream << "(";
    for (int i = 0; i < type_->numFields(); i++)
    {
      if (i > 0) stream << ", ";
      stream << fields_[i];
    }
    stream << ")";

    return String::create(stream.str().c_str());
  }
  
  void RecordObject::reach()
  {
    type_.reach();
    
    for (int i = 0; i < type_->numFields(); i++)
    {
      fields_[i].reach();
    }
  }
  
  gc<ClassObject> StringObject::getClass(VM& vm) const
  {
    return vm.stringClass();
  }
  
  void StringObject::reach()
  {
    value_.reach();
  }
}
