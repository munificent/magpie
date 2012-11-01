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

  gc<Object> DynamicObject::create(gc<ClassObject> classObj)
  {
    ASSERT(classObj->numFields() == 0, "Class cannot have fields.");
    
    // Allocate enough memory for the object.
    void* mem = Memory::allocate(sizeof(DynamicObject));

    // Construct it by calling global placement new.
    return ::new(mem) DynamicObject(classObj);
  }

  gc<Object> DynamicObject::create(ArrayView<gc<Object> >& args)
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
  
  gc<ClassObject> FunctionObject::getClass(VM& vm) const
  {
    return vm.functionClass();
  }

  gc<String> FunctionObject::toString() const
  {
    // TODO(bob): Do something better here.
    return String::create("[fn]");
  }
  
  void FunctionObject::reach()
  {
    chunk_.reach();
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
