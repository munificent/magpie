#pragma once

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Method;
  
  // A single Magpie module.
  class Module
  {
  public:
    void reach();
    
  private:    
    NO_COPY(Module);
  };
}
