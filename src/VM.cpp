#include "VM.h"

namespace magpie {
  VM::VM()
  : memory_(*this, 1024 * 1024 * 10), // TODO(bob): Use non-magic number.
    fiber_(new (*this) Fiber(*this)) {}
}

