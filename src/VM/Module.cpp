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
}

