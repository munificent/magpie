#pragma once

#include "Test.h"

namespace magpie
{
  class QueueTests : public Test
  {
  public:
    static void run();
    
  private:
    static void enqueueDequeue();
    static void serialEnqueue();
    static void multipleEnqueue();
    static void count();
    static void subscript();
  };
}

