#pragma once

#include "Test.h"

namespace magpie
{
  class TokenTests : public Test
  {
  public:
    virtual void runTests();

  private:
    void create();
    void is();
  };
}

