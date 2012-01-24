#pragma once

#include <iostream>

#include "Managed.h"

namespace magpie
{
  // Garbage-collected immutable string class.
  class String : public Managed
  {
  public:
    // Creates a new heap-allocated string containing the given characters.
    // If the length is known (or you want to truncate `text`, it can be passed
    // in. Otherwise omit it and it will be calculated from `text`.
    static temp<String> create(const char* text, int length = -1);

    // Gets the character at the given index.
    const char operator [](int index) const;

    // Compare strings.
    bool operator ==(const String& right) const;
    bool operator !=(const String& right) const;

    bool operator ==(const char* right) const;
    bool operator !=(const char* right) const;

    // Gets the number of characters in the string.
    int length() const;

    // Gets the raw character array for the string. Returns a reference to
    // a zero-length string, not `NULL`, if the string is empty. Callers must
    // not retain a reference to the returned string: it points directly at
    // data on the managed heap and may be moved or garbage collected.
    const char* cString() const;

    // Creates a new string containing a substring of this one. `start` is the
    // index of the first character and `end` is the index of just past the
    // the last character to include in the substring.
    temp<String> substring(int start, int end) const;

    virtual void trace(std::ostream& out) const;

  private:
    static size_t calcStringSize(int length);

    String(const char* text, int length);

    int length_;
    char chars_[FLEXIBLE_SIZE];
  };

  // Compare to raw C strings.
  inline bool operator ==(const char* left, const String& right)
  {
    return right == left;
  }

  inline bool operator !=(const char* left, const String& right)
  {
    return right != left;
  }
}

