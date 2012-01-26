#pragma once

#include "Array.h"
#include "Callframe.h"
#include "GC.h"
#include "Managed.h"

namespace magpie
{
  class VM;

  class Fiber : public Managed
  {
  public:
    Fiber(VM& vm);

    // TODO(bob): Implement reach().

    void interpret(gc<Method> method);
    
  private:
    void run();
    void call(gc<Method> method);
    
    // Loads a register for the given callframe.
    inline gc<Managed> load(const CallFrame& frame, int reg)
    {
      return stack_[frame.stackStart() + reg];
    }
    
    // Stores a register for the given callframe.
    inline void store(const CallFrame& frame, int reg, gc<Managed> value)
    {
      stack_[frame.stackStart() + reg] = value;
    }
    
    VM&                 vm_;
    Array<gc<Managed> > stack_;
    Array<CallFrame>    callFrames_;

    NO_COPY(Fiber);
  };
}