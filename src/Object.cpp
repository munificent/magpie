#include "Object.h"

#include "NumberObject.h"

namespace magpie {
  using std::ostream;

  gc<Object> Object::create(VM& vm, double value) {
    return gc<Object>(new (vm) NumberObject(value));
  }
  
  std::ostream & operator<<(std::ostream & out, const Object & object)
  {
    object.debugTrace(out);
    return out;
  }
  
}
