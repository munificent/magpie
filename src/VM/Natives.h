#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

#define NATIVE(name) gc<Object> name##Native(VM& vm, Fiber& fiber, ArrayView<gc<Object> >& args, NativeResult& result)

namespace magpie
{
  NATIVE(objectClass);
  NATIVE(objectNew);
  NATIVE(objectToString);
  NATIVE(printString);
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
  NATIVE(channelNew);
  NATIVE(channelReceive);
  NATIVE(channelSend);
  NATIVE(functionCall);
  NATIVE(listAdd);
  NATIVE(listCount);
  NATIVE(listIndex);
  NATIVE(listIndexSet);
  NATIVE(listInsert);
}

