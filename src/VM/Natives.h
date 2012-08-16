#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

#define NATIVE(name) gc<Object> name##Native(ArrayView<gc<Object> >& args)

namespace magpie
{
  NATIVE(print);
  NATIVE(numPlusNum);
  NATIVE(stringPlusString);
  NATIVE(numMinusNum);
  NATIVE(numTimesNum);
  NATIVE(numDivNum);
  NATIVE(stringCount);
  NATIVE(numToString);
}

