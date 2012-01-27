#include "Method.h"

namespace magpie
{
  temp<Method> Method::create()
  {
    return Memory::makeTemp(new Method());
  }
  
  int Method::addConstant(gc<Object> constant)
  {
    constants_.add(constant);
    return constants_.count() - 1;
  }

  gc<Object> Method::getConstant(int index) const
  {
    ASSERT_INDEX(index, constants_.count());
    return constants_[index];
  }

  void Method::write(instruction code)
  {
    code_.add(code);
  }
}