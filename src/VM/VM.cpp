#include "VM.h"

namespace magpie
{
  VM::VM()
  : fiber_()
  {
    Memory::initialize(this, 1024 * 1024 * 10); // TODO(bob): Use non-magic number.
    
    AllocScope scope;
    fiber_ = Fiber::create(*this);
  }

  void VM::reachRoots()
  {
    Memory::reach(fiber_);
  }
}

