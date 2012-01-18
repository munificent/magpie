#pragma once

#include <iostream>

namespace magpie {
  // Garbage-collected immutable string class.
  class String {
  public:
  };
  
    /*
  using std::ostream;
  
  // Garbage-collected immutable string class.
  class String {
  public:
    // Creates a new string using the given C-style format string and a
    // number of arguments to be formatted.
    static String Format(const char* format, ...);
    
    String() {}
    
    String(const char* chars);
    
    explicit String(char c);
    
    String(const String & other)
    :   mData(other.mData)
    {}
    
    // Comparison operators.
    bool         operator < (const String & other) const;
    bool         operator <=(const String & other) const;
    bool         operator > (const String & other) const;
    bool         operator >=(const String & other) const;
    bool         operator ==(const String & other) const;
    bool         operator !=(const String & other) const;
    
    // Subscript operator to get characters from the string.
    const char & operator [](int index) const;
    
    // Concatenation operators.
    String       operator + (const String & other) const;
    String &     operator +=(const String & other);
    String       operator + (char other) const;
    String &     operator +=(char other);
    
    // Gets the raw character array for the string. Returns a reference to
    // a zero-length string, not NULL, if the string is empty.
    const char * CString() const;
    
    // Gets the number of characters in the string.
    int Length() const;
    
    // Gets the position in this string of the given substring or -1 if not
    // found.
    int IndexOf(const String & other, int startIndex = 0) const;
    
    // Replaces every instance of `from` in the string with `to`.
    String Replace(const String & from, const String & to) const;
    
    // Gets the hash code for the string.
    unsigned int HashCode() const;
    
    int CompareTo(const String & other) const;
    
    String Substring(int startIndex) const;
    String Substring(int startIndex, int count) const;
    
    static unsigned int Fnv1Hash(const char * text);
    
  private:
    struct StringData
    {
      StringData(const char * text);
      
      int          length;
      const char * chars;
      int          hashCode;
    };
    
    String(const String & left, const String & right);
    String(const char * text, bool isOnHeap);
    
    void Init(const char * text, bool isOnHeap);
    
    static const int FormattedStringMax = 512;
    
    // The hash code of a zero-character string. This constant comes from
    // the FNV1 hash algorithm used to hash strings.
    static const unsigned int EmptyStringHash = 0x811c9dc5;
    
    static const char * sEmptyString;
    
    Ref<StringData> mData;
  };
  
  bool operator ==(const char * left, const String & right);
  bool operator !=(const char * left, const String & right);
  
  bool operator ==(const String & left, const char * right);
  bool operator !=(const String & left, const char * right);
  
  ostream & operator <<(ostream & cout, const String & string);
  */
}

