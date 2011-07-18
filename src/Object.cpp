#include "Object.h"

#include "NumberObject.h"

namespace magpie {
  
  Ref<Object> Object::create(double value) {
    return Ref<Object>(new NumberObject(value));
  }
  
}
