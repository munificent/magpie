#pragma once

#include "Test.h"

namespace magpie
{
  class StringTests : public Test
  {
  public:
    virtual void runTests();

  private:
    void create();
    void concat();
    void subscript();
    void equals();
    void substring();
  };
}

