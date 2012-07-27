#include "Object.h"
#include "Natives.h"

namespace magpie
{
  NATIVE(print)
  {
    std::cout << args[0] << std::endl;
    return args[0];
  }

  NATIVE(add)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() + args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(subtract)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() - args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(multiply)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() * args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(divide)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() / args[1]->toNumber();
    return new NumberObject(c);
  }
}

