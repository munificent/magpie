#include "Object.h"

namespace magpie
{
  using std::ostream;

  void ClassObject::reach()
  {
    Memory::reach(name_);
  }
  
  RecordType::RecordType(gc<String> signature)
  : signature_(signature)
  {
    // Count the fields.
    numFields_ = 0;
    for (int i = 0; i < signature->length(); i++)
    {
      if ((*signature)[i] == ':') numFields_++;
    }
  }
  
  void RecordType::reach()
  {
    Memory::reach(signature_);
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

  void RecordObject::reach()
  {
    Memory::reach(type_);
    
    for (int i = 0; i < type_->numFields(); i++)
    {
      Memory::reach(fields_[i]);
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
    Memory::reach(value_);
  }
}
