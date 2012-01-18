#include <stdio.h>
#include <stdarg.h>
#include <cstring>

#include "Macros.h"
#include "MagpieString.h"

namespace magpie {
  const char * String::emptyString_ = "";
  
  void String::create(Memory & memory, const char * text,
                     gc<String> & outString) {
    int length = strlen(text);
    outString.set(new(memory) String(text, length));
  }

  size_t String::getSize() const {
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
    return sizeof(String) + (sizeof(char) * length);
  }

  String::String(const char * text, int length)
  : length_(length) {
    strncpy(chars_, text, length);
  }
}

