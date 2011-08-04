#pragma once

#include "Fiber.h"
#include "GC.h"
#include "Macros.h"
#include "Memory.h"
#include "RootSource.h"

namespace magpie {
  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource {
  public:
    VM();
    
    virtual void reachRoots(Memory& memory);
    
    Memory&    getMemory() { return memory_; }
    gc<Fiber>& getFiber() { return fiber_; }
    
  private:
    Memory    memory_;
    gc<Fiber> fiber_;
    
    NO_COPY(VM);
  };
}

