#include <iostream>

#include "LexerTests.h"
#include "MemoryTests.h"
#include "QueueTests.h"
#include "StringTests.h"
#include "TokenTests.h"

int main (int argc, char * const argv[])
{
  using namespace magpie;

  LexerTests().run();
  MemoryTests().run();
  QueueTests().run();
  StringTests().run();
  TokenTests().run();

  Test::showResults();
  return 0;
}
