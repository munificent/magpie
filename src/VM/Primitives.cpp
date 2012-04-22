#include "Object.h"
#include "Primitives.h"

namespace magpie
{
  PRIMITIVE(print)
  {
    std::cout << arg << std::endl;
    // TODO(bob): Is this what print() should return?
    return arg;
  }
}

