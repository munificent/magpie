#include "Object.h"

#include "NumberObject.h"

namespace magpie {
  using std::ostream;

  Ref<Object> Object::create(double value) {
    return Ref<Object>(new NumberObject(value));
  }
  
  std::ostream & operator<<(std::ostream & out, const Object & object)
  {
    object.debugTrace(out);
    return out;
  }
  
}
