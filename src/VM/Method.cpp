#include "Method.h"
#include "MagpieString.h"

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
  
  void MethodScope::add(gc<String> name, gc<Method> method)
  {
    names_.add(name);
    methods_.add(method);
  }
  
  gc<Method> MethodScope::findMain() const
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      if (*names_[i] == "main") return methods_[i];
    }
    
    return gc<Method>();
  }
}