#include "Object.h"
#include "Primitives.h"

namespace magpie
{
  PRIMITIVE(print)
  {
    std::cout << args[0] << std::endl;
    return args[0];
  }

  PRIMITIVE(add)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() + args[1]->toNumber();
    return new NumberObject(c);
  }
  
  PRIMITIVE(subtract)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() - args[1]->toNumber();
    return new NumberObject(c);
  }
  
  PRIMITIVE(multiply)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() * args[1]->toNumber();
    return new NumberObject(c);
  }
  
  PRIMITIVE(divide)
  {
    // TODO(bob): Handle non-number types.
    double c = args[0]->toNumber() / args[1]->toNumber();
    return new NumberObject(c);
  }
}

