#include "VM.h"
#include "Object.h"
#include "Primitives.h"

#define DEF_PRIMITIVE(name) \
        { \
          temp<String> primName = String::create(#name); \
          methods_.define(primName, name##Primitive); \
        }

namespace magpie
{
  VM::VM()
  : fiber_()
  {
    Memory::initialize(this, 1024 * 1024 * 10); // TODO(bob): Use non-magic number.
    
    AllocScope scope;
    fiber_ = Fiber::create(*this);
    
    DEF_PRIMITIVE(print);
    
    // TODO(bob): Get rid of this nasty conversion problem.
    temp<Object> trueTemp = BoolObject::create(true);
    temp<Object> falseTemp = BoolObject::create(false);
    true_ = trueTemp;
    false_ = falseTemp;
  }

  void VM::reachRoots()
  {
    Memory::reach(fiber_);
    Memory::reach(true_);
    Memory::reach(false_);
  }
}

