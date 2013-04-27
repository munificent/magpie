#include "ArrayTests.h"
#include "Data/Array.h"

namespace magpie
{
  // Number-like value class that has a meaningful default constructor.
  struct Int
  {
    Int()
    : value(-1)
    {}

    Int(int value)
    : value(value)
    {}

    int value;
  };

  bool operator !=(int left, const Int& right)
  {
    return left != right.value;
  }

  std::ostream& operator <<(std::ostream& out, const Int& i)
  {
    out << i.value;
    return out;
  };

  void ArrayTests::runTests()
  {
    create();
    subscript();
    lastIndexOf();
    removeAt();
    grow();
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

  void ArrayTests::grow()
  {
    Array<Int> array;
    array.add(1);
    array.add(2);

    // Grow to a smaller size does nothing.
    array.grow(1);
    EXPECT_EQUAL(2, array.count());
    EXPECT_EQUAL(1, array[0]);
    EXPECT_EQUAL(2, array[1]);

    // Grow to same size does nothing.
    array.grow(2);
    EXPECT_EQUAL(2, array.count());
    EXPECT_EQUAL(1, array[0]);
    EXPECT_EQUAL(2, array[1]);

    // Grow.
    array.grow(4);
    EXPECT_EQUAL(4, array.count());
    EXPECT_EQUAL(1, array[0]);
    EXPECT_EQUAL(2, array[1]);
    EXPECT_EQUAL(-1, array[2]);
    EXPECT_EQUAL(-1, array[3]);
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

