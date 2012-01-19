#include "StringTests.h"
#include "MagpieString.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  void StringTests::run() {
    create();
  }
  
  void StringTests::create() {
    AllocScope scope;
    
    temp<String> s = String::create("some text");
    temp<String> s2 = String::create("more");
    
    EXPECT_EQUAL(9, s->length());
    EXPECT_EQUAL(0, strcmp("some text", s->cString()));
    
    EXPECT_EQUAL(4, s2->length());
    EXPECT_EQUAL(0, strcmp("more", s2->cString()));
  }
}

