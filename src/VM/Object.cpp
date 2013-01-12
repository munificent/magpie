#include <sstream>

#include "Array.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  using std::ostream;

  bool Object::equal(gc<Object> a, gc<Object> b)
  {
    // If they have the same identity, they must be equal.
    if (a.sameAs(b)) return true;

    // TODO(bob): Use double-dispatch pattern here?
    // Different types are never equal since they can have different methods
    // and thus different user-visible behavior.
    if (a->type() != b->type()) return false;

    // Same type, so compare values.
    switch (a->type())
    {
      case OBJECT_BOOL:
        return a->toBool() == b->toBool();

      case OBJECT_CHARACTER:
        return a->asCharacter()->value() == b->asCharacter()->value();

      case OBJECT_FLOAT:
        return a->asFloat() == b->asFloat();

      case OBJECT_INT:
        return a->asInt() == b->asInt();

      case OBJECT_NOTHING:
        ASSERT(false, "Should only be one instance of nothing.");
        break;

      case OBJECT_RECORD:
        ASSERT(false, "Equality on records not implemented.");
        break;

      case OBJECT_STRING:
        return a->asString() == b->asString();
        break;

      case OBJECT_CHANNEL:
      case OBJECT_CLASS:
      case OBJECT_DYNAMIC:
      case OBJECT_FILE:
      case OBJECT_FUNCTION:
      case OBJECT_LIST:
        // Equality is based on identity, so if we get here, they
        // aren't equal.
        return false;
    }

    UNREACHABLE();
    return false;
  }
  
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

    // If nothing is going to receive the "done". Just ignore it and close
    // immediately.
    if (receivers_.count() == 0) return false;
    
    // Send "done" to all of the receivers.
    for (int i = 0; i < receivers_.count(); i++)
    {
      receivers_[i]->storeReturn(vm.getBuiltIn(BUILT_IN_DONE));
      receivers_[i]->ready();
    }

    receivers_.clear();

    // Add the sender back to the scheduler after the receivers so it can
    // continue.
    sender->ready();
    
    return true;
  }

  gc<Object> ChannelObject::receive(VM& vm, gc<Fiber> receiver)
  {
    // If the channel is closed, immediately receive 'done'.
    if (!isOpen_)
    {
      return vm.getBuiltIn(BUILT_IN_DONE);
    }

    // If we have a sender, take its value.
    if (senders_.count() > 0)
    {
      gc<Fiber> sender = senders_.removeAt(0);

      // TODO(bob): Nasty. Do something cleaner to downcast here.
      gc<ChannelSendSuspension> suspension = static_cast<ChannelSendSuspension*>(
          &(*sender->ready()));
      return suspension->value();
    }

    // Otherwise, suspend.
    receivers_.add(receiver);
    return NULL;
  }

  void ChannelObject::send(gc<Fiber> sender, gc<Object> value)
  {
    // TODO(bob): What if the channel is closed?

    // If we have a receiver, give it the value.
    if (receivers_.count() > 0)
    {
      gc<Fiber> receiver = receivers_.removeAt(0);
      receiver->storeReturn(value);
      receiver->ready();

      // Add the sender back to the scheduler too since it isn't blocked.
      sender->ready();
      return;
    }

    // Otherwise, stuff the value and suspend.
    sender->suspend(new ChannelSendSuspension(value));
    senders_.add(sender);
    return;
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
    senders_.reach();
    receivers_.reach();
  }

  gc<ClassObject> CharacterObject::getClass(VM& vm) const
  {
    return vm.characterClass();
  }

  gc<String> CharacterObject::toString() const
  {
    // TODO(bob): This will probably barf on non-ASCII stuff. Fix.
    return String::format("%c", value_);
  }

  gc<ClassObject> ClassObject::create(gc<String> name, int numFields,
      int numSuperclasses, const ArrayView<gc<Object> >& superclasses)
  {
    // Allocate enough memory for the record and its fields.
    void* mem = Memory::allocate(sizeof(ClassObject) +
        sizeof(gc<ClassObject>) * (numSuperclasses - 1));

    // Construct it by calling global placement new.
    gc<ClassObject> classObj = ::new(mem) ClassObject(name, numFields,
                                                      numSuperclasses);

    // Initialize the superclasses.
    for (int i = 0; i < numSuperclasses; i++)
    {
      classObj->superclasses_[i] = superclasses[i]->asClass();
    }

    return classObj;
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

    for (int i = 0; i < numSuperclasses_; i++)
    {
      superclasses_[i].reach();
    }
  }

  bool ClassObject::is(const ClassObject& other) const
  {
    if (this == &other) return true;

    // Walk the class hierarchy.
    // TODO(bob): Slow! Do something constant time here, like a binary matrix
    // of superclasses.
    for (int i = 0; i < numSuperclasses_; i++)
    {
      if (superclasses_[i]->is(other)) return true;
    }
    
    return false;
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

  void FileObject::open(gc<Fiber> fiber, gc<String> path)
  {
    fiber->scheduler().openFile(fiber, path);
  }

  void FileObject::read(gc<Fiber> fiber)
  {
    fiber->scheduler().read(fiber, this);
  }

  void FileObject::close(gc<Fiber> fiber)
  {
    ASSERT(isOpen_, "IO library should not call close on a closed file.");
    // Mark the file closed immediately so other fibers can't try to use it.
    isOpen_ = false;

    fiber->scheduler().closeFile(fiber, this);
  }

  gc<ClassObject> FileObject::getClass(VM& vm) const
  {
    return vm.fileClass();
  }

  gc<String> FileObject::toString() const
  {
    // TODO(bob): Include some kind of ID or something here.
    return String::create("[file]");
  }

  void FileObject::reach()
  {
    // TODO(bob): How should we handle file_ here?
  }

  gc<ClassObject> FloatObject::getClass(VM& vm) const
  {
    return vm.floatClass();
  }

  gc<String> FloatObject::toString() const
  {
    return String::format("%g", value_);
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

  gc<ClassObject> IntObject::getClass(VM& vm) const
  {
    return vm.intClass();
  }

  gc<String> IntObject::toString() const
  {
    return String::format("%d", value_);
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
