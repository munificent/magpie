#pragma once

#include "Test.h"

namespace magpie
{
  class ArrayTests : public Test
  {
  public:
    virtual void runTests();

  private:
    void create();
    void subscript();
    void lastIndexOf();
    void removeAt();
    void grow();
    void truncate();
  };
}

