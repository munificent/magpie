#include "Object.h"

namespace magpie
{
  using std::ostream;

  void ClassObject::reach()
  {
    Memory::reach(name_);
  }
  
  void StringObject::reach()
  {
    Memory::reach(value_);
  }
}
