#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

#define PRIMITIVE(name) gc<Object> name##Primitive(ArrayView<gc<Object> >& args)

namespace magpie
{
  PRIMITIVE(print);
  PRIMITIVE(add);
  PRIMITIVE(subtract);
  PRIMITIVE(multiply);
  PRIMITIVE(divide);
}

