#pragma once

#include "Test.h"

namespace magpie
{
  class FixedQueueTests : public Test
  {
  public:
    virtual void runTests();
    
  private:
    void enqueueDequeue();
    void serialEnqueue();
    void multipleEnqueue();
    void count();
    void subscript();
  };
}

