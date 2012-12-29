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
  NATIVE(stringPlusString);
  NATIVE(intPlusInt);
  NATIVE(intPlusFloat);
  NATIVE(floatPlusInt);
  NATIVE(floatPlusFloat);
  NATIVE(intMinusInt);
  NATIVE(intMinusFloat);
  NATIVE(floatMinusInt);
  NATIVE(floatMinusFloat);
  NATIVE(intTimesInt);
  NATIVE(intTimesFloat);
  NATIVE(floatTimesInt);
  NATIVE(floatTimesFloat);
  NATIVE(intDivInt);
  NATIVE(intDivFloat);
  NATIVE(floatDivInt);
  NATIVE(floatDivFloat);
  NATIVE(intModInt);
  NATIVE(intCompareToInt);
  NATIVE(intCompareToFloat);
  NATIVE(floatCompareToInt);
  NATIVE(floatCompareToFloat);
  NATIVE(stringCount);
  NATIVE(stringSubscriptInt);
  NATIVE(floatToString);
  NATIVE(intToString);
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
  NATIVE(listSubscriptInt);
  NATIVE(listSubscriptRange);
  NATIVE(listSubscriptSetInt);
  NATIVE(listInsert);
}

