#pragma once

#include <iostream>

#include "Common.h"

namespace magpie
{
  class Memory;

  // Interface for a class that provides the root objects reachable by the
  // garbage collector. When a collection needs to occur, Memory starts by
  // calling this to find the known root objects. An implementor should
  // override reachRoots() and call memory.reach() on it for each root object.
  class RootSource
  {
  public:
    virtual ~RootSource() {}

    virtual void reachRoots() = 0;
  };
}

