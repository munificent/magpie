#pragma once

#include "Test.h"

namespace magpie
{
  class LexerTests : public Test
  {
  public:
    virtual void runTests();

  private:
    void create();
    void stringLiteral();
  };
}

