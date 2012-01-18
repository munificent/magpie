#include "StringTests.h"
#include "MagpieString.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  struct TestRootedString : public RootSource {
    virtual void reachRoots(Memory& memory) {
      memory.reach(string);
    }
    
    gc<String> string;
  };
  
  void StringTests::run() {
    create();
  }
  
  void StringTests::create() {
    TestRootedString roots;
    Memory memory(roots, 1000);
    
    String::create(memory, "some text", roots.string);
    
    EXPECT_EQUAL(9, roots.string->length());
    EXPECT_EQUAL(0, strcmp("some text", roots.string->cString()));
  }
}

