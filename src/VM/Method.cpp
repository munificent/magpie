#include "Method.h"

namespace magpie
{
  temp<Method> Method::create()
  {
    return Memory::makeTemp(new Method());
  }
  
  int Method::addConstant(gc<Managed> constant)
  {
    constants_.add(constant);
    return constants_.count() - 1;
  }

  void Method::write(instruction code)
  {
    code_.add(code);
  }
}