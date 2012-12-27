#pragma once

#include <iostream>
#include <cstring>

#include "Macros.h"
#include "Memory.h"

namespace magpie
{
  // A resizable dynamic array class. Array items must support copying and a
  // default constructor.
  template <class T>
  class Array
  {
  public:
    Array()
    : count_(0),
      capacity_(0),
      items_(NULL) {}

    Array(int capacity)
    : count_(0),
      capacity_(0),
      items_(NULL)
    {
      ensureCapacity_(capacity);
    }

    Array(int size, const T& fillWith)
    : count_(0),
      capacity_(0),
      items_(NULL)
    {
      ensureCapacity_(size);

      for (int i = 0; i < size; i++) items_[i] = fillWith;
      count_ = size;
    }

    Array(const Array<T>& array)
    : count_(0),
      capacity_(0),
      items_(NULL)
    {
      addAll(array);
    }

    ~Array()
    {
      clear();
    }

    // Gets the number of items currently in the array.
    int count() const { return count_; }

    // Gets whether or not the array is empty.
    bool isEmpty() const { return count_ == 0; }

    // Gets the maximum number of array the stack can hold before a
    // reallocation will occur.
    int capacity() const { return capacity_; }

    // Adds the given item to the end of the array, increasing its size
    // automatically.
    void add(const T& value)
    {
      ensureCapacity_(count_ + 1);
      items_[count_++] = value;
    }

    // Adds all of the items from the given array to this one.
    void addAll(const Array<T>& array)
    {
      ensureCapacity_(count_ + array.count_);

      for (int i = 0; i < array.count_; i++) items_[count_++] = array[i];
    }

    // Inserts the given item at the given index, growing the array and pushing
    // following items down.
    void insert(const T& value, int index)
    {
      if (index < 0) index = count_ + 1 + index;
      ASSERT_INDEX(index, count_ + 1);

      ensureCapacity_(count_ + 1);

      // Shift items down.
      for (int i = count_; i > index; i--) items_[i] = items_[i - 1];

      items_[index] = value;
      count_++;
    }

    // Removes all items from the array.
    void clear()
    {
      items_ = NULL;
      count_ = 0;
      capacity_ = 0;
    }

    // Removes the item at the given index. Indexes are zero-based from the
    // beginning of the array. Negative indexes are from the end of the array
    // and go forward, so that -1 is the last item in the array.
    T removeAt(int index)
    {
      if (index < 0) index = count_ + index;
      ASSERT_INDEX(index, count_);

      T item = items_[index];
      
      // Shift items up.
      for (int i = index; i < count_ - 1; i++) items_[i] = items_[i + 1];

      // Clear the copy of the last item.
      items_[count_ - 1] = T();
      count_--;

      return item;
    }

    // Finds the index of the given item in the array. Returns -1 if not found.
    int indexOf(const T& value) const
    {
      for (int i = 0; i < count_; i++)
      {
        if (items_[i] == value) return i;
      }

      return -1;
    }

    int lastIndexOf(const T& value) const
    {
      for (int i = count_ - 1; i >= 0; i--)
      {
        if (items_[i] == value) return i;
      }

      return -1;
    }

    void truncate(int count)
    {
      ASSERT(count >= 0, "Cannot truncate to a negative count.");

      // Early out if there's nothing to remove.
      if (count >= count_) return;

      // Clear the items.
      for (int i = count; i < count_; i++)
      {
        items_[i] = T();
      }

      // TODO(bob): This never actually reallocates a smaller array.
      // Should it?

      // Truncate.
      count_ = count;
    }

    // If the array is smaller than size, than grows it to that size. New
    // elements are filled by calling the default constructor on T.
    void grow(int size)
    {
      if (count_ >= size) return;
      ensureCapacity_(size);

      // Default construct any new ones.
      for (int i = count_; i < size; i++)
      {
        new (static_cast<void*>(&items_[i])) T();
      }

      count_ = size;
    }

    // Indicates that the given array of objects is reachable and should be
    // preserved during garbage collection. T should be a gc type.
    void reach()
    {
      for (int i = 0; i < count_; i++)
      {
        items_[i].reach();
      }
    }

    // Assigns the contents of the given array to this one. Clears this array
    // and refills it with the contents of the other.
    Array& operator=(const Array& other)
    {
      // Early out of self-assignment.
      if (&other == this) return *this;

      clear();
      addAll(other);

      return *this;
    }

    // Gets the item at the given index. Indexes are zero-based from the
    // beginning of the array. Negative indexes are from the end of the array
    // and go forward, so that -1 is the last item in the array.
    T& operator[] (int index)
    {
      if (index < 0) index = count_ + index;
      ASSERT_INDEX(index, count_);

      return items_[index];
    }

    // Gets the item at the given index. Indexes are zero-based from the
    // beginning of the array. Negative indexes are from the end of the array
    // and go forward, so that -1 is the last item in the array.
    const T& operator[] (int index) const
    {
      if (index < 0) index = count_ + index;
      ASSERT_INDEX(index, count_);

      return items_[index];
    }

    // Reverses the order of the items in the array.
    void reverse()
    {
      for (int i = 0; i < count_ / 2; i++)
      {
        T temp = items_[i];
        items_[i] = items_[count_ - i - 1];
        items_[count_ - i - 1] = temp;
      }
    }

  private:
    void ensureCapacity_(int desiredCapacity)
    {
      // Early out if we have enough capacity.
      if (capacity_ >= desiredCapacity) return;

      // Figure out the new array size.
      // Instead of growing to just the capacity we need, we'll grow by a
      // multiple of the current size. This ensures amortized O(n) complexity
      // on adding instead of O(n^2).
      int capacity = capacity_;
      if (capacity < MIN_CAPACITY) capacity = MIN_CAPACITY;

      while (capacity < desiredCapacity) capacity *= GROW_FACTOR;

      // Create the new array.
      void* mem = Memory::allocate(sizeof(T) * capacity);

      // Copy the existing items over. Note that this does *not* call any
      // user-defined assignment operators. It just moves the memory straight
      // over.
      memcpy(mem, items_, sizeof(T) * count_);
      items_ = static_cast<T*>(mem);
      capacity_ = capacity;
    }

    static const int MIN_CAPACITY = 16;
    static const int GROW_FACTOR  = 2;

    int count_;
    int capacity_;
    T*  items_;
  };

  template <class T>
  class ArrayView
  {
  public:
    ArrayView(Array<T>& array, int start)
    : array_(array),
      start_(start)
    {}

    // Gets the item at the given index.
    inline const T& operator[] (int index) const
    {
      return array_[start_ + index];
    }

    // Gets the item at the given index.
    inline T& operator[] (int index)
    {
      return array_[start_ + index];
    }

  private:
    Array<T>& array_;
    int start_;
  };
}