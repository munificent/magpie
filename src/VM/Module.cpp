#include "Module.h"
#include "Memory.h"
#include "Method.h"
#include "Object.h"

namespace magpie
{
  void Module::reach()
  {
    Memory::reach(body_);
    Memory::reach(variables_);
    Memory::reach(variableNames_);
  }
  
  void Module::bindBody(gc<Chunk> body)
  {
    ASSERT(body_.isNull(), "Can only bind a module once.");
    body_ = body;
  }
  
  void Module::addVariable(gc<String> name, gc<Object> value)
  {
    variableNames_.add(name);
    variables_.add(value);
  }
  
  int Module::findVariable(gc<String> name)
  {
    for (int i = 0; i < variableNames_.count(); i++)
    {
      if (variableNames_[i] == name) return i;
    }
    
    return -1;
  }
  
  void Module::setVariable(int index, gc<Object> value)
  {
    variables_[index] = value;
  }
}

