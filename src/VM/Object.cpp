#include "Object.h"

#include "NumberObject.h"

namespace magpie
{
  using std::ostream;

  temp<NumberObject> Object::create(double value)
  {
    return Memory::makeTemp(new NumberObject(value));
  }

  std::ostream& operator <<(std::ostream& out, const Object& object)
  {
    object.debugTrace(out);
    return out;
  }
}
