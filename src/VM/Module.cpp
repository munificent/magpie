#include "Module.h"
#include "Memory.h"
#include "Method.h"
#include "Object.h"

namespace magpie
{
  void Module::reach()
  {
    Memory::reach(body_);
    Memory::reach(exports_);
    Memory::reach(exportNames_);
  }
  
  void Module::bindBody(gc<Method> body)
  {
    ASSERT(body_.isNull(), "Can only bind a module once.");
    body_ = body;
  }
  
  void Module::addExport(gc<String> name, gc<Object> value)
  {
    exportNames_.add(name);
    exports_.add(value);
  }
  
  gc<Object> Module::getImport(int importIndex, int exportIndex)
  {
    return imports_[importIndex]->getExport(exportIndex);
  }
}

