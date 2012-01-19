#include <iostream>

#include "LexerTests.h"
#include "StringTests.h"
#include "TokenTests.h"

int main (int argc, char * const argv[]) {
  using namespace magpie;
  
  LexerTests::run();
  StringTests::run();
  TokenTests::run();
  
  Test::showResults();
  return 0;
}
