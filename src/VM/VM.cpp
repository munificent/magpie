#include "VM.h"

namespace magpie {

  VM::VM()
  : memory_(*this, 1024 * 1024 * 10), // TODO(bob): Use non-magic number.
    fiber_() {
    AllocScope scope(memory_);
    fiber_.set(new (scope) Fiber(*this));
  }
  
  void VM::reachRoots(Memory& memory) {
    memory.reach(fiber_);
  }

}

