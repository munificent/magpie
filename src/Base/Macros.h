#pragma once

#include <iostream>
#include <cstdlib>

#define ASSERT(condition, message)                  \
  if (!(condition))                                 \
  {                                                 \
    std::cout << "ASSERTION FAILED " << __FILE__    \
              << ":" << __LINE__ << " - "           \
              << message << std::endl;              \
    abort();                                        \
  }

#define ASSERT_NOT_NULL(value)                      \
  ASSERT(value != NULL, "Expression " #value " cannot be null.")

#define ASSERT_INDEX(index, max)                    \
  if (((index) < 0) || ((index) >= max))            \
  {                                                 \
    std::cout << "ASSERTION FAILED " << __FILE__    \
              << ":" << __LINE__ << " - "           \
              << "Index " << index                  \
              << " was out of range [0, " << max    \
              << ")." << std::endl;                 \
    abort();                                        \
  }

#define ASSERT_STRING_NOT_EMPTY(value)              \
  ASSERT(value.Length() > 0, "String " #value " cannot be empty.")

// Use this inside a class declaration to prevent the compiler from creating
// the default copy constructor and assignment operators for the class. Note
// that this starts a private section, so you should either use this at the end
// of the declaration or before a privacy declaration.
#define NO_COPY(className)                          \
  private:                                          \
    className(const className &);                   \
    className& operator=(const className &)

// Use this inside a class declaration to prevent the class from being
// dynamically allocated on the heap. Note that starts a private section, so
// you should either use this at the end of the declaration or before a privacy
// declaration.
#define STACK_ONLY                                  \
  void* operator new(size_t size);                  \
  void operator delete(void*, size_t);              \

// This is used to indicate that an array member in a class is flexibly-sized.
#define FLEXIBLE_SIZE (1)

// Statically ensures that T is a supertype of S (i.e. S can be assigned to T).
// Will generate a compile error if this isn't the case. Note that both types
// must be complete (i.e. fully declared) at the point that you use this macro.
// It will barf if either type has just been forward-declared.
#define CHECK_SUBTYPE(T, S)                                 \
while (false) {                                             \
  *(static_cast<T* volatile*>(0)) = static_cast<S*>(0);     \
}
