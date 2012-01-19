#pragma once

#include <iostream>

#include "Managed.h"

namespace magpie {
  // Garbage-collected immutable string class.
  class String : public Managed {
  public:
    static temp<String> create(AllocScope& scope, const char* text);
    
    virtual size_t allocSize() const;
    
    // Gets the number of characters in the string.
    int length() const;
    
    // Gets the raw character array for the string. Returns a reference to
    // a zero-length string, not `NULL`, if the string is empty. Callers must
    // not retain a reference to the returned string: it points directly at
    // data on the managed heap and may be moved or garbage collected.
    const char* cString() const;
    
  private:
    static const char* emptyString_;
    
    static size_t calcStringSize(int length);
    
    String(const char* text, int length);
    
    int length_;
    char chars_[FLEXIBLE_SIZE];
  };
}

