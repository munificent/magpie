#include "StringTests.h"
#include "MagpieString.h"

namespace magpie {
  void StringTests::run() {
    /*
    TestEmpty();
    TestFromChar();
    TestLength();
    TestSubscript();
    TestAddition();
    TestAssignment();
    TestCompoundAssignment();
    TestComparison();
    TestSubstring();
    TestReplace();
    */
  }
  
  /*
  void StringTests::TestEmpty() {
    String a;
    
    EXPECT_EQUAL(0, a.Length());
    EXPECT_EQUAL('\0', *a.CString());
  }
  
  void StringTests::TestFromChar() {
    String a = String('b');
    EXPECT_EQUAL("b", a);
  }
  
  void StringTests::TestLength() {
    String a = "";
    EXPECT_EQUAL(0, a.Length());
    
    String b = "b";
    EXPECT_EQUAL(1, b.Length());
    
    String c = "some string";
    EXPECT_EQUAL(11, c.Length());
  }
  
  void StringTests::TestSubscript() {
    String a = "abcd";
    EXPECT_EQUAL('a', a[0]);
    EXPECT_EQUAL('b', a[1]);
    EXPECT_EQUAL('c', a[2]);
    EXPECT_EQUAL('d', a[3]);
  }
  
  void StringTests::TestAddition() {
    {
      String a = "left";
      String b = "right";
      
      String c = a + b;
      EXPECT_EQUAL("leftright", c);
    }
    
    // left is null
    {
      String a;
      String b = "right";
      
      String c = a + b;
      EXPECT_EQUAL("right", c);
    }
    
    // right is null
    {
      String a = "left";
      String b;
      
      String c = a + b;
      EXPECT_EQUAL("left", c);
    }
    
    // both null
    {
      String a;
      String b;
      
      String c = a + b;
      EXPECT_EQUAL(0, c.Length());
    }
  }
  
  void StringTests::TestAssignment() {
    String a = "abc";
    String b = a;
    
    EXPECT_EQUAL("abc", b);
  }
  
  void StringTests::TestCompoundAssignment() {
    String a = "abc";
    a += String("def");
    
    EXPECT_EQUAL("abcdef", a);
  }
  
  void StringTests::TestComparison() {
    String a1 = "abc";
    String a2 = "abc";
    String b  = "abd";
    
    EXPECT_EQUAL(false, a1 <  a2);
    EXPECT_EQUAL(true,  a1 <= a2);
    EXPECT_EQUAL(false, a1 >  a2);
    EXPECT_EQUAL(true,  a1 >= a2);
    EXPECT_EQUAL(true,  a1 == a2);
    EXPECT_EQUAL(false, a1 != a2);
    
    EXPECT_EQUAL(true,  a1 <  b);
    EXPECT_EQUAL(true,  a1 <= b);
    EXPECT_EQUAL(false, a1 >  b);
    EXPECT_EQUAL(false, a1 >= b);
    EXPECT_EQUAL(false, a1 == b);
    EXPECT_EQUAL(true, a1 != b);
  }
  
  void StringTests::TestSubstring() {
    String a = "abcdef";
    
    // starting index
    EXPECT_EQUAL("abcdef", a.Substring(0));
    EXPECT_EQUAL("cdef", a.Substring(2));
    
    // start from end
    EXPECT_EQUAL("ef", a.Substring(-2));
    
    // start and count
    EXPECT_EQUAL("abcdef", a.Substring(0, 6));
    EXPECT_EQUAL("cdef", a.Substring(2, 4));
    EXPECT_EQUAL("cde", a.Substring(2, 3));
    EXPECT_EQUAL("f", a.Substring(5, 1));
    EXPECT_EQUAL("", a.Substring(0, 0));
    
    // start from end and count
    EXPECT_EQUAL("ab", a.Substring(-6, 2));
    EXPECT_EQUAL("cdef", a.Substring(-4, 4));
    EXPECT_EQUAL("f", a.Substring(-1, 1));
    
    // start and distance from end
    EXPECT_EQUAL("abcde", a.Substring(0, -1));
    EXPECT_EQUAL("ab", a.Substring(0, -4));
    EXPECT_EQUAL("", a.Substring(2, -4));
    
    // start frome end and distance from end
    EXPECT_EQUAL("abcde", a.Substring(-6, -1));
    EXPECT_EQUAL("bcd", a.Substring(-5, -2));
  }
  
  void StringTests::TestReplace() {
    EXPECT_EQUAL("not found", String("not found").Replace("blah", "foo"));
    
    EXPECT_EQUAL("fleginning", String("beginning").Replace("be", "fle"));
    EXPECT_EQUAL("at ending", String("at end").Replace("end", "ending"));
    
    EXPECT_EQUAL("xbaybazba", String("xcyczc").Replace("c", "ba"));
  }
  */
}

