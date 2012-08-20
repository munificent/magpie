#include "Module.h"
#include "Memory.h"
#include "Method.h"
#include "Object.h"

namespace magpie
{
  void Module::reach()
  {
    body_.reach();
    variables_.reach();
    variableNames_.reach();
  }
  
  void Module::setBody(gc<Chunk> body)
  {
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

