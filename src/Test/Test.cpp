#include <iostream>

#include "Test.h"

namespace magpie
{
  using std::cout;
  using std::endl;

  int Test::tests_ = 0;
  int Test::failed_ = 0;

  void Test::showResults()
  {
    if (failed_ == 0)
    {
      cout << "SUCCESS: All " << tests_ << " tests passed." << endl;
    }
    else
    {
      cout << endl;
      cout << "FAILURE: " << (tests_ - failed_) <<
              " tests passed out of " << tests_ << "." << endl;
    }
  }
}

