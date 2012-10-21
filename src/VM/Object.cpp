#include "Object.h"

namespace magpie
{
  using std::ostream;

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
    gc<ClassObject> classObj = args[0]->toClass();
    
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

  gc<Object> DynamicObject::getField(int index)
  {
    ASSERT_INDEX(index, class_->numFields());
    return fields_[index];
  }

  void ListObject::trace(std::ostream& stream) const
  {
    stream << "[";
    for (int i = 0; i < elements_.count(); i++)
    {
      stream << elements_[i];
      if (i < elements_.count() - 1) stream << ", ";
    }
    stream << "]";
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

  void RecordObject::reach()
  {
    type_.reach();
    
    for (int i = 0; i < type_->numFields(); i++)
    {
      fields_[i].reach();
    }
  }
  
  void RecordObject::trace(std::ostream& stream) const
  {
    // TODO(bob): Handle named fields.
    stream << "(";
    for (int i = 0; i < type_->numFields(); i++)
    {
      if (i > 0) stream << ", ";
      stream << fields_[i];
    }
    stream << ")";
  }
  
  void StringObject::reach()
  {
    value_.reach();
  }
}
