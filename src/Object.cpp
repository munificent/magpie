#include "Object.h"

#include "NumberObject.h"

namespace magpie {
  using std::ostream;

  gc<Object> Object::create(Memory& memory, double value) {
    return gc<Object>(new (memory) NumberObject(value));
  }
  
  std::ostream & operator<<(std::ostream & out, const Object & object)
  {
    object.debugTrace(out);
    return out;
  }
  
}
