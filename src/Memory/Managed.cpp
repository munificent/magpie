#include "Managed.h"

#include "VM.h"
#include "Memory.h"

namespace magpie
{
  void* Managed::operator new(size_t s)
  {
    return Memory::allocate(s);
  }
}