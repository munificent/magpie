#pragma once

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  // A single Magpie module.
  class Module : public Managed
  {
  public:
    virtual void reachRoots();

  private:

    NO_COPY(Module);
  };
}

