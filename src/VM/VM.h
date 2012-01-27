#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie
{
  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource
  {
  public:
    VM();

    virtual void reachRoots();

    Fiber& fiber() { return *fiber_; }

  private:
    gc<Fiber> fiber_;

    NO_COPY(VM);
  };
}

