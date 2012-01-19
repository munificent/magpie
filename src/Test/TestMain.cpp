#include <iostream>

#include "StringTests.h"
#include "TokenTests.h"

int main (int argc, char * const argv[]) {
  using namespace magpie;
  
  StringTests::run();
  TokenTests::run();
  
  Test::showResults();
  return 0;
}
