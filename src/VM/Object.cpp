#include "Object.h"

namespace magpie
{
  using std::ostream;

  temp<NumberObject> Object::create(double value)
  {
    return Memory::makeTemp(new NumberObject(value));
  }
}
