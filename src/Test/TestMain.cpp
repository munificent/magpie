#include <iostream>

#include "StringTests.h"

int main (int argc, char * const argv[]) {
  using namespace magpie;
  
  StringTests::run();
  
  Test::showResults();
  return 0;
}
