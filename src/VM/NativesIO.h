#pragma once

#include "Fiber.h"
#include "Macros.h"
#include "Memory.h"

namespace magpie
{
  NATIVE(bindIO);
  NATIVE(fileClose);
  NATIVE(fileIsOpen);
  NATIVE(fileOpen);
  NATIVE(fileSize);
  NATIVE(fileReadBytesInt);
  NATIVE(fileStreamBytes);
  NATIVE(bufferNewSize);
  NATIVE(bufferCount);
  NATIVE(bufferSubscriptInt);
  NATIVE(bufferSubscriptSetInt);
  NATIVE(bufferDecodeAscii);
}

