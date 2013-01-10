#include <sstream>

#include "Managed.h"
#include "VM.h"
#include "Memory.h"

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
