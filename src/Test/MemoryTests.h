#pragma once

#include "Test.h"

namespace magpie
{
  class MemoryTests : public Test
  {
  public:
    virtual void runTests();

  private:
    void collect();
    void inScopeTempsArePreserved();
  };
}

