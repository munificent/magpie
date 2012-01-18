#include "VM.h"

namespace magpie {

  VM::VM()
  : memory_(*this, 1024 * 1024 * 10), // TODO(bob): Use non-magic number.
    fiber_(new (memory_) Fiber(*this)) {}
  
  void VM::reachRoots(Memory& memory) {
    memory.reach(fiber_);
  }

}

