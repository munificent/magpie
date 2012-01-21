#include <iostream>

#include "Test.h"

namespace magpie
{
  using std::cout;
  using std::endl;

  int Test::tests_ = 0;
  int Test::failed_ = 0;

  struct TestRoot : public RootSource
  {
    virtual void reachRoots()
    {
      // No roots.
    }
  };
  
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
  
  void Test::run()
  {
    // Set up a heap for this suite.
    TestRoot root;
    Memory::initialize(&root, 1024 * 1024 * 10);
    
    runTests();
    
    Memory::shutDown();
  }
}

