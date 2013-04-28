#include <iostream>

#include "ArrayTests.h"
#include "LexerTests.h"
#include "MemoryTests.h"
#include "FixedQueueTests.h"
#include "StringTests.h"
#include "TokenTests.h"

int main (int argc, char * const argv[])
{
  using namespace magpie;

  ArrayTests().run();
  LexerTests().run();
  MemoryTests().run();
  FixedQueueTests().run();
  StringTests().run();
  TokenTests().run();

  Test::showResults();
  return 0;
}
