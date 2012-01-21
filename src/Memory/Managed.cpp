#include "Managed.h"

#include "VM.h"
#include "Memory.h"

namespace magpie
{
  void Managed::trace(std::ostream& out) const
  {
    out << "<Managed>";
  }

  void* Managed::operator new(size_t s)
  {
    return Memory::allocate(s);
  }
}