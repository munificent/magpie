#include "VM.h"
#include "Object.h"
#include "Primitives.h"

#define DEF_PRIMITIVE(name) \
        methods_.define(String::create(#name), name##Primitive); \

namespace magpie
{
  VM::VM()
  : fiber_()
  {
    Memory::initialize(this, 1024 * 1024 * 10); // TODO(bob): Use non-magic number.
    
    fiber_ = new Fiber(*this);
    
    DEF_PRIMITIVE(print);
    
    true_ = new BoolObject(true);
    false_ = new BoolObject(false);
  }

  void VM::reachRoots()
  {
    methods_.reach();
    Memory::reach(fiber_);
    Memory::reach(true_);
    Memory::reach(false_);
  }
}

