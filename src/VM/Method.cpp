#include "Method.h"

namespace magpie
{
  temp<Method> Method::create(const Array<instruction>& code,
                              const Array<gc<Object> >& constants,
                              int numRegisters)
  {
    return Memory::makeTemp(new Method(code, constants, numRegisters));
  }
  
  gc<Object> Method::getConstant(int index) const
  {
    ASSERT_INDEX(index, constants_.count());
    return constants_[index];
  }
}