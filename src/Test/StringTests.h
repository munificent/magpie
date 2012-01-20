#pragma once

#include "Test.h"

namespace magpie
{
  class StringTests : public Test
  {
  public:
    static void run();

  private:
    static void create();
    static void subscript();
    static void equals();
    static void substring();
  };
}

