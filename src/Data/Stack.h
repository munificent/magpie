#pragma once

#include <iostream>

#include "Common.h"
#include "Data/Array.h"

namespace magpie
{

  // A simple stack of items with a variable capacity. Implemented using a
  // dynamic array. Push and pop are O(1) (amortized). Stack items must support
  // a default constructor and copying.
  template <class T>
  class Stack
  {
  public:
    Stack()
    : items_() {}

    // Gets the number of items currently in the stack.
    int count() const { return items_.count(); }

    // Gets whether or not the stack is empty.
    bool isEmpty() const { return items_.isEmpty(); }

    // Pushes the given item onto the top of the stack.
    void push(const T & item) { items_.Add(item); }

    // Pops the top item off the stack.
    T pop()
    {
      ASSERT(!isEmpty(), "Cannot pop an empty stack.");

      T popped = items_[-1];
      items_.Remove(-1);

      return popped;
    }

    // Returns the item on the top of the stack without removing it.
    T & peek()
    {
      ASSERT(!isEmpty(), "Cannot peek an empty stack.");
      return items_[-1];
    }

    // Gets the item at the given index in the stack. Index zero is the
    // top of the stack, and indices increase towards the bottom of the
    // stack.
    T & operator[] (int index)
    {
      ASSERT_INDEX(index, count());
      return items_[-1 - index];
    }

  private:
    Array<T> items_;

    NO_COPY(Stack);
  };

}
