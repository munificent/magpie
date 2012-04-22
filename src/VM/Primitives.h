#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

#define PRIMITIVE(name) gc<Object> name##Primitive(gc<Object> arg)

namespace magpie
{
  PRIMITIVE(print);
}

