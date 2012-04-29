#include "ArrayTests.h"
#include "Array.h"

namespace magpie
{
  void ArrayTests::runTests()
  {
    create();
    subscript();
    lastIndexOf();
    removeAt();
    truncate();
  }
  
  void ArrayTests::create()
  {
    {
      Array<int> array;
      
      EXPECT_EQUAL(0, array.count());
      EXPECT_EQUAL(0, array.capacity());
    }
    
    {
      Array<int> array(5);
      
      EXPECT_EQUAL(0, array.count());
      EXPECT(array.capacity() >= 5);
    }
  }
  
  void ArrayTests::subscript()
  {
    Array<int> array;
    array.add(1);
    array.add(2);
    array.add(3);
    
    EXPECT_EQUAL(1, array[0]);
    EXPECT_EQUAL(2, array[1]);
    EXPECT_EQUAL(3, array[2]);
    EXPECT_EQUAL(1, array[-3]);
    EXPECT_EQUAL(2, array[-2]);
    EXPECT_EQUAL(3, array[-1]);
  }
  
  void ArrayTests::lastIndexOf()
  {
    Array<char> array;
    array.add('a');
    array.add('b');
    array.add('c');
    array.add('b');
    array.add('e');
    
    EXPECT_EQUAL(3, array.lastIndexOf('b'));
    EXPECT_EQUAL(2, array.lastIndexOf('c'));
    EXPECT_EQUAL(-1, array.lastIndexOf('z'));
  }
  
  void ArrayTests::removeAt()
  {
    Array<char> array;
    array.add('a');
    array.add('b');
    array.add('c');
    
    EXPECT_EQUAL(3, array.count());
    
    array.removeAt(1);

    EXPECT_EQUAL(2, array.count());
    EXPECT_EQUAL('a', array[0]);
    EXPECT_EQUAL('c', array[1]);
    
    array.removeAt(-2); // negative is from end
    
    EXPECT_EQUAL(1, array.count());
    EXPECT_EQUAL('c', array[0]);
    
    array.removeAt(0);
    
    EXPECT_EQUAL(0, array.count());
  }
  
  void ArrayTests::truncate()
  {
    Array<char> array;
    array.add('a');
    array.add('b');
    array.add('c');
    array.add('d');
    array.add('e');
    
    EXPECT_EQUAL(5, array.count());
    
    // Truncate to greater size does nothing.
    array.truncate(7);
    EXPECT_EQUAL(5, array.count());
    
    // Truncate to same size does nothing.
    array.truncate(5);
    EXPECT_EQUAL(5, array.count());
    
    array.truncate(3);
    EXPECT_EQUAL(3, array.count());
    EXPECT_EQUAL('a', array[0]);
    EXPECT_EQUAL('b', array[1]);
    EXPECT_EQUAL('c', array[2]);

    array.truncate(0);
    EXPECT_EQUAL(0, array.count());
  }
}

