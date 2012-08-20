#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

#define NATIVE(name) gc<Object> name##Native(VM& vm, ArrayView<gc<Object> >& args)

namespace magpie
{
  NATIVE(print);
  NATIVE(numPlusNum);
  NATIVE(stringPlusString);
  NATIVE(numMinusNum);
  NATIVE(numTimesNum);
  NATIVE(numDivNum);
  NATIVE(numLessThanNum);
  NATIVE(numLessThanEqualToNum);
  NATIVE(numGreaterThanNum);
  NATIVE(numGreaterThanEqualToNum);
  NATIVE(stringCount);
  NATIVE(numToString);
  NATIVE(listCount);
  NATIVE(listIndex);
}

