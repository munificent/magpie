#pragma once

#include "Test.h"

namespace magpie
{
  class ParserTests : public Test
  {
  public:
    virtual void runTests();

  private:
    void parseModule();
    void parseList();
    void testCode(const char*, const char*);
  };
}

