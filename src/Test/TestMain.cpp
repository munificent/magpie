#include <iostream>

#include "LexerTests.h"
#include "QueueTests.h"
#include "StringTests.h"
#include "TokenTests.h"

int main (int argc, char * const argv[])
{
  using namespace magpie;

  TestRoot root;
  Memory::initialize(&root, 1024 * 1024 * 10);

  LexerTests::run();
  QueueTests::run();
  StringTests::run();
  TokenTests::run();

  Test::showResults();
  return 0;
}
