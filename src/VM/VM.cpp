#include "VM.h"

namespace magpie {

  VM::VM()
  : fiber_(NULL) {
    Memory::initialize(this, 1024 * 1024 * 10); // TODO(bob): Use non-magic number.
    fiber_ = gc<Fiber>(new Fiber(*this));
  }
  
  void VM::reachRoots() {
    Memory::reach(fiber_);
  }

}

