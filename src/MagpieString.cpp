#include <stdio.h>
#include <stdarg.h>
#include <cstring>

#include "Macros.h"
#include "MagpieString.h"

namespace magpie {
  /*
  const char * String::sEmptyString = "";
  
  String::StringData::StringData(const char * text)
  :   chars(text)
  {
    length = strlen(text);
    hashCode = Fnv1Hash(text);
  }
  
  String String::Format(const char* format, ...)
  {
    char result[FormattedStringMax];
    
    va_list args;
    va_start (args, format);
    
    vsprintf(result, format, args);
    
    va_end (args);
    
    return String(result);
  }
  
  String::String(const char* chars)
  {
    Init(chars, false);
  }
  
  String::String(char c)
  {
    char chars[2];
    chars[0] = c;
    chars[1] = '\0';
    
    Init(chars, false);
  }
  
  bool String::operator <(const String & other) const
  {
    return strcmp(CString(), other.CString()) < 0;
  }
  
  bool String::operator <=(const String & other) const
  {
    return strcmp(CString(), other.CString()) <= 0;
  }
  
  bool String::operator >(const String & other) const
  {
    return strcmp(CString(), other.CString()) > 0;
  }
  
  bool String::operator >=(const String & other) const
  {
    return strcmp(CString(), other.CString()) >= 0;
  }
  
  bool String::operator ==(const String & other) const
  {
    if (this == &other) return true;
    if (mData == other.mData) return true;
    
    // if the hashes don't match, the strings must be different
    if (HashCode() != other.HashCode()) return false;
    
    return strcmp(CString(), other.CString()) == 0;
  }
  
  bool String::operator !=(const String & other) const
  {
    return !(*this == other);
  }
  
  const char & String::operator[] (int index) const
  {
    ASSERT_RANGE(index, Length() + 1); // allow accessing the terminator
    
    if (mData.IsNull()) return sEmptyString[0];
    return mData->chars[index];
  }
  
  String String::operator +(const String & other) const
  {
    return String(*this, other);
  }
  
  String & String::operator +=(const String & other)
  {
    *this = *this + other;
    return *this;
  }
  
  String String::operator +(char other) const
  {
    return String(*this, String(other));
  }
  
  String & String::operator +=(char other)
  {
    *this = *this + String(other);
    return *this;
  }
  
  const char* String::CString() const
  {
    if (mData.IsNull()) return sEmptyString;
    
    return mData->chars;
  }
  
  int String::Length() const
  {
    if (mData.IsNull()) return 0;
    
    return mData->length;
  }
  
  int String::IndexOf(const String & other, int startIndex) const
  {
    if (mData.IsNull()) return -1;
    
    // Keep the start index in bounds.
    if (startIndex >= Length()) startIndex = Length() - 1;
    
    const char* found = strstr(mData->chars + startIndex, other.CString());
    if (found == NULL) return -1;
    
    return static_cast<int>(found - mData->chars);
  }
  
  String String::Replace(const String & from, const String & to) const
  {
    // TODO(bob): Could certainly be optimized.
    String result;
    
    int start = 0;
    while (start < Length())
    {
      int index = IndexOf(from, start);
      if (index != -1)
      {
        result += Substring(start, index - start) + to;
        start = index + from.Length();
      }
      else
      {
        result += Substring(start, Length() - start);
        break;
      }
    }
    
    return result;
  }
  
  unsigned int String::HashCode() const
  {
    if (mData.IsNull()) return EmptyStringHash;
    
    return mData->hashCode;
  }
  
  int String::CompareTo(const String & other) const
  {
    return strcmp(CString(), other.CString());
  }
  
  String String::Substring(int startIndex) const
  {
    if (startIndex < 0)
    {
      // count from end
      startIndex = Length() + startIndex;
    }
    
    ASSERT_RANGE(startIndex, Length());
    
    int length = Length() - startIndex;
    char* heap = new char[length + 1];
    strcpy(heap, CString() + startIndex);
    
    return String(heap, true);
  }
  
  String String::Substring(int startIndex, int count) const
  {
    if (startIndex < 0)
    {
      // count from end
      startIndex = Length() + startIndex;
    }
    
    if (count < 0)
    {
      count = Length() + count - startIndex;
    }
    
    ASSERT_RANGE(startIndex, Length());
    ASSERT(startIndex + count <= Length(), "Range must not go past end of string.");
    
    char* heap = new char[count + 1];
    strncpy(heap, CString() + startIndex, count);
    heap[count] = '\0';
    
    return String(heap, true);
  }
  
  String::String(const String & left, const String & right)
  {
    // make a new buffer on the heap
    int length = left.Length() + right.Length();
    char* heap = new char[length + 1];
    
    // concatenate the strings
    strcpy(heap, left.CString());
    strcpy(&heap[left.Length()], right.CString());
    
    Init(heap, true);
  }
  
  String::String(const char * text, bool isOnHeap)
  {
    Init(text, isOnHeap);
  }
  
  void String::Init(const char * text, bool isOnHeap)
  {
    if (isOnHeap)
    {
      mData = Ref<StringData>(new StringData(text));
    }
    else
    {
      // hoist it onto the heap
      int length = strlen(text);
      char * heap = new char[length + 1];
      strcpy(heap, text);
      
      mData = Ref<StringData>(new StringData(heap));
    }
  }
  
  unsigned int String::Fnv1Hash(const char * text)
  {
    // magical number!
    const unsigned int fnvPrime = 0x01000193;
    
    // treat as unsigned
    const unsigned char * byte = reinterpret_cast<const unsigned char *>(text);
    
    unsigned int hash = EmptyStringHash;
    
    while (*byte != '\0')
    {
      // multiply by the 32 bit FNV magic prime mod 2^32
      hash *= fnvPrime;
      
      // xor the bottom with the current octet
      hash ^= static_cast<unsigned int>(*byte);
      byte++;
    }
    
    return hash;
  }
  
  bool operator ==(const char * left, const String & right)
  {
    // if the hashes don't match, the strings must be different
    if (String::Fnv1Hash(left) != right.HashCode()) return false;
    
    return strcmp(left, right.CString()) == 0;
  }
  
  bool operator !=(const char * left, const String & right)
  {
    // if the hashes don't match, the strings must be different
    if (String::Fnv1Hash(left) != right.HashCode()) return true;
    
    return strcmp(left, right.CString()) != 0;
  }
  
  bool operator ==(const String & left, const char * right)
  {
    // if the hashes don't match, the strings must be different
    if (left.HashCode() != String::Fnv1Hash(right)) return false;
    
    return strcmp(left.CString(), right) == 0;
  }
  
  bool operator !=(const String & left, const char * right)
  {
    // if the hashes don't match, the strings must be different
    if (left.HashCode() != String::Fnv1Hash(right)) return true;
    
    return strcmp(left.CString(), right) != 0;
  }
  
  ostream & operator <<(ostream & cout, const String & string)
  {
    cout << string.CString();
    return cout;
  }
   */
}

