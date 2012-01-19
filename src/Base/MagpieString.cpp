#include <stdio.h>
#include <stdarg.h>
#include <cstring>

#include "Macros.h"
#include "MagpieString.h"

namespace magpie {
  const char* String::emptyString_ = "";
  
  temp<String> String::create(const char* text) {
    int length = strlen(text);
    // Allocate enough memory for the string and its character array.
    void* mem = Memory::allocate(calcStringSize(length));
    // Construct it by calling global placement new.
    return Memory::makeTemp(::new(mem) String(text, length));
  }

  size_t String::allocSize() const {
    return calcStringSize(length_);
  }
  
  int String::length() const
  {
    return length_;
  }
  
  const char* String::cString() const
  {
    return chars_;
  }
  
  size_t String::calcStringSize(int length) {
    // Note that sizeof(String) includes one extra byte because the flex
    // array is declared with that size. We need that extra byte for the
    // terminator. Otherwise, we'd want to do "* (length + 1)".
    return sizeof(String) + (sizeof(char) * length);
  }

  String::String(const char * text, int length)
  : length_(length) {
    // Add one for the terminator.
    strncpy(chars_, text, length + 1);
  }
}

