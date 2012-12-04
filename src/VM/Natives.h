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
  NATIVE(bindCore);
  NATIVE(bindIO);
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
  NATIVE(channelClose);
  NATIVE(channelIsOpen);
  NATIVE(channelNew);
  NATIVE(channelReceive);
  NATIVE(channelSend);
  NATIVE(fileClose);
  NATIVE(fileIsOpen);
  NATIVE(fileOpen);
  NATIVE(fileRead);
  NATIVE(functionCall);
  NATIVE(listAdd);
  NATIVE(listCount);
  NATIVE(listIndex);
  NATIVE(listIndexSet);
  NATIVE(listInsert);
}

