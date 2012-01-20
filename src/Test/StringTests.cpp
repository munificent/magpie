#include "StringTests.h"
#include "MagpieString.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  void StringTests::run() {
    create();
    subscript();
    equals();
    substring();
  }
  
  void StringTests::create() {
    AllocScope scope;
    
    temp<String> s1 = String::create("some text");
    temp<String> s2 = String::create("more");
    
    EXPECT_EQUAL(9, s1->length());
    EXPECT_EQUAL("some text", *s1);
    
    EXPECT_EQUAL(4, s2->length());
    EXPECT_EQUAL("more", *s2);
  }
  
  void StringTests::subscript() {
    AllocScope scope;
    
    temp<String> s = String::create("abcd");
    
    EXPECT_EQUAL('a', (*s)[0]);
    EXPECT_EQUAL('b', (*s)[1]);
    EXPECT_EQUAL('c', (*s)[2]);
    EXPECT_EQUAL('d', (*s)[3]);
  }
  
  void StringTests::equals() {
    AllocScope scope;
    
    temp<String> s = String::create("something");
    temp<String> same = String::create("something");
    temp<String> different = String::create("different");
    
    // String to C-string.
    EXPECT(*s == "something");
    EXPECT_FALSE(*s != "something");
    EXPECT_FALSE(*s == "else");
    EXPECT(*s != "else");
    
    // String to String.
    EXPECT(*s == *same);
    EXPECT_FALSE(*s != *same);
    EXPECT_FALSE(*s == *different);
    EXPECT(*s != *different);
    
    // C-string to String.
    EXPECT("something" == *s);
    EXPECT_FALSE("something" != *s);
    EXPECT_FALSE("else" == *s);
    EXPECT("else" != *s);
  }
  
  void StringTests::substring() {
    AllocScope scope;
    
    temp<String> s = String::create("abcdef");
    
    // Zero-length.
    temp<String> sub = s->substring(3, 3);
    EXPECT_EQUAL("", *sub);
    
    // From beginning.
    sub = s->substring(0, 2);
    EXPECT_EQUAL("ab", *sub);
    
    // In middle.
    sub = s->substring(2, 5);
    EXPECT_EQUAL("cde", *sub);
    
    // To end.
    sub = s->substring(4, 6);
    EXPECT_EQUAL("ef", *sub);
  }
}

