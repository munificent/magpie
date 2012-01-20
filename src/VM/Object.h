#pragma once

#include <iostream>

#include "GC.h"
#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Memory;
  class Multimethod;
  class NumberObject;

  class Object : public Managed
  {
  public:
    static gc<Object> create(double value);

    Object() : Managed() {}

    virtual Multimethod*  asMultimethod() { return NULL; }
    virtual NumberObject* asNumber()      { return NULL; }

    // TODO(bob): Debug only.
    virtual void debugTrace(std::ostream& stream) const = 0;

  private:
    NO_COPY(Object);
  };

  // TODO(bob): Debug only.
  std::ostream& operator <<(std::ostream& out, const Object& object);
}