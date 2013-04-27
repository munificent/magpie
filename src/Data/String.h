#pragma once

#include <iostream>

#include "Common.h"
#include "Data/Array.h"
#include "Memory/Managed.h"

namespace magpie
{
  // TODO(bob): Is this the best place for this?
  typedef int symbolId;
  typedef int methodId;

  // Garbage-collected immutable string class.
  class String : public Managed
  {
  public:
    // Creates a new heap-allocated string containing the given characters.
    // If the length is known (or you want to truncate `text`), it can be passed
    // in. Otherwise omit it and it will be calculated from `text`.
    static gc<String> create(const char* text, int length = -1);

    // Creates a new heap-allocated string containing the given array of
    // characters.
    static gc<String> create(const Array<char>& text);

    // Creates a new string using the given C-style format string and a
    // number of arguments to be formatted.
    static gc<String> format(const char* format, ...);

    // Creates a new string that is the concatenation of [a] and [b].
    static gc<String> concat(gc<String> a, gc<String> b);

    // Gets the character at the given index.
    char operator [](int index) const;

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
    gc<String> substring(int start, int end) const;

    // Creates a new string where every instance of [from] in this string has
    // been replaced with [to].
    gc<String> replace(char from, char to) const;

    virtual void trace(std::ostream& out) const;

  private:
    static const int FORMATTED_STRING_MAX = 512;

    static size_t calcStringSize(int length);

    String(int length);

    int length_;
    char chars_[FLEXIBLE_SIZE];

    NO_COPY(String);
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

