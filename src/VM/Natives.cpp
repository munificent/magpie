#include "Object.h"
#include "Natives.h"

namespace magpie
{
  NATIVE(print)
  {
    std::cout << args[0] << std::endl;
    return args[0];
  }

  NATIVE(addNum)
  {
    double c = args[0]->toNumber() + args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(addString)
  {
    return new StringObject(
        String::concat(args[0]->toString(), args[1]->toString()));
  }
  
  NATIVE(subtract)
  {
    double c = args[0]->toNumber() - args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(multiply)
  {
    double c = args[0]->toNumber() * args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(divide)
  {
    double c = args[0]->toNumber() / args[1]->toNumber();
    return new NumberObject(c);
  }
}

