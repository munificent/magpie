#include <sstream>

#include "Memory/Managed.h"
#include "Memory/Memory.h"
#include "VM/VM.h"

namespace magpie
{
  gc<String> Managed::toString() const
  {
    // TODO(bob): Hackish, but handy for debugging.
    std::stringstream result;
    trace(result);
    return String::create(result.str().c_str());
  }

  void Managed::trace(std::ostream& out) const
  {
    out << "<Managed>";
  }

  void* Managed::operator new(size_t s)
  {
    return Memory::allocate(s);
  }
}
