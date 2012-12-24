#include <cstdio>
#include <cstdarg>
#include <cstring>

#include "Macros.h"
#include "MagpieString.h"

namespace magpie
{
  gc<String> String::create(const char* text, int length)
  {
    if (length == -1) length = static_cast<int>(strlen(text));

    // Allocate enough memory for the string and its character array.
    void* mem = Memory::allocate(calcStringSize(length));

    // Construct it by calling global placement new.
    gc<String> string = ::new(mem) String(length);

    strncpy(string->chars_, text, length);

    // Make sure its terminated. May not be, for example, when creating a
    // string from a substring.
    string->chars_[length] = '\0';

    return string;
  }

  gc<String> String::create(const Array<char>& text)
  {
    // Allocate enough memory for the string and its character array.
    void* mem = Memory::allocate(calcStringSize(text.count()));

    // Construct it by calling global placement new.
    gc<String> string = ::new(mem) String(text.count());

    // TODO(bob): memmove?
    for (int i = 0; i < text.count(); i++) {
      string->chars_[i] = text[i];
    }

    // Make sure its terminated.
    string->chars_[text.count()] = '\0';

    return string;
  }

  gc<String> String::format(const char* format, ...)
  {
    char result[FORMATTED_STRING_MAX];

    va_list args;
    va_start (args, format);

    vsprintf(result, format, args);

    va_end (args);

    return String::create(result);
  }

  gc<String> String::concat(gc<String> a, gc<String> b)
  {
    int length = a->length() + b->length();
    // Allocate enough memory for the string and its character array.
    void* mem = Memory::allocate(calcStringSize(length));

    // Construct it by calling global placement new.
    gc<String> string = ::new(mem) String(length);

    strncpy(string->chars_, a->chars_, a->length());
    strncpy(string->chars_ + a->length(), b->chars_, b->length());

    // Make sure its terminated.
    string->chars_[length] = '\0';

    return string;
  }

  char String::operator [](int index) const
  {
    ASSERT_INDEX(index, length() + 1); // Allow accessing the terminator.

    return chars_[index];
  }

  bool String::operator ==(const String& right) const
  {
    // Check for identity.
    if (this == &right) return true;

    if (length_ != right.length_) return false;

    // TODO(bob): Compare hashcodes if we have them.

    return strncmp(chars_, right.chars_, length_) == 0;
  }

  bool String::operator !=(const String& right) const
  {
    return !(*this == right);
  }

  bool String::operator ==(const char* right) const
  {
    return strcmp(chars_, right) == 0;
  }

  bool String::operator !=(const char* right) const
  {
    return !(*this == right);
  }

  int String::length() const
  {
    return length_;
  }

  const char* String::cString() const
  {
    return chars_;
  }

  gc<String> String::substring(int start, int end) const
  {
    ASSERT_INDEX(start, length());
    ASSERT_INDEX(end, length() + 1); // End is past the last character.
    ASSERT(start <= end, "Start must come before end.");

    return create(&chars_[start], end - start);
  }

  gc<String> String::replace(char from, char to) const
  {
    gc<String> result = String::create(chars_, length_);

    for (int i = 0; i < length_; i++)
    {
      if (result->chars_[i] == from) result->chars_[i] = to;
    }

    return result;
  }

  void String::trace(std::ostream& out) const
  {
    out << chars_;
  }

  size_t String::calcStringSize(int length)
  {
    // Note that sizeof(String) includes one extra byte because the flex
    // array is declared with that size. We need that extra byte for the
    // terminator. Otherwise, we'd want to do "* (length - 1)".
    return sizeof(String) + (sizeof(char) * length);
  }

  String::String(int length)
  : length_(length)
  {
  }

  std::ostream& operator <<(std::ostream& out, const String& right)
  {
    out << right.cString();
    return out;
  };
}

