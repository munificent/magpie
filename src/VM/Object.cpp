#include "Object.h"

#include "NumberObject.h"

namespace magpie {
  using std::ostream;

  gc<Object> Object::create(AllocScope& scope, double value) {
    return gc<Object>(new (scope) NumberObject(value));
  }
  
  std::ostream & operator<<(std::ostream & out, const Object & object)
  {
    object.debugTrace(out);
    return out;
  }
  
}
