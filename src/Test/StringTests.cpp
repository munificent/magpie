#include "StringTests.h"
#include "MagpieString.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  void StringTests::run() {
    create();
  }
  
  void StringTests::create() {
    TestRoot root;
    Memory memory(root, 1000);
    AllocScope scope(memory);
    
    temp<String> s = String::create(scope, "some text");
    
    EXPECT_EQUAL(9, s->length());
    EXPECT_EQUAL(0, strcmp("some text", s->cString()));
  }
}

