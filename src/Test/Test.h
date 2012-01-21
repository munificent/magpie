#pragma once

#include <iostream>

#include "GC.h"
#include "Memory.h"
#include "RootSource.h"

#define EXPECT(condition) \
_expect(__FILE__, __LINE__, #condition, condition)

#define EXPECT_FALSE(condition) \
_expect(__FILE__, __LINE__, #condition, !(condition))

#define EXPECT_MSG(condition, message) \
_expectMsg(__FILE__, __LINE__, condition, message)

#define EXPECT_EQUAL(expected, actual) \
_expectEqual(__FILE__, __LINE__, #actual, expected, actual)

namespace magpie
{
  using std::cout;
  using std::endl;

  // Base class for test classes.
  class Test
  {
  public:
    static void showResults();
    
    virtual ~Test() {}
    
    void run();
    virtual void runTests() = 0;
    
  protected:
    static void _expect(const char * file, int line,
                        const char * expression,
                        bool condition)
                        {
      tests_++;
      if (!condition)
      {
        cout << "FAIL: " << expression << " was false." << endl;
        cout << "      " << file << ":" << line << endl;
        failed_++;
      }
    }

    static void _expectMsg(const char * file, int line,
                           bool condition, const char * message)
                           {
      tests_++;
      if (!condition)
      {
        cout << "FAIL: " << message << endl;
        cout << "      " << file << ":" << line << endl;
        failed_++;
      }
    }

    template <typename Left, typename Right>
    static void _expectEqual(const char* file, int line,
                             const char* actualExpression,
                             const Left & expected, const Right & actual)
                             {
      tests_++;
      if (expected != actual)
      {
        cout << "FAIL: Expected " << actualExpression << " to be " <<
        expected << ", but was " << actual << "." << endl;
        cout << "      " << file << ":" << line << endl;
        failed_++;
      }
    }

  private:
    static int tests_;
    static int failed_;
  };
}

