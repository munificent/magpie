#pragma once

#include "Array.h"
#include "Managed.h"
#include "Memory.h"
#include "Method.h"

namespace magpie
{
  class Object;
  class VM;

  class Fiber : public Managed
  {
  public:
    static temp<Fiber> create(VM& vm);
    
    // TODO(bob): Implement reach().

    temp<Object> interpret(gc<Method> method);
    
  private:
    struct CallFrame
    {
      // So that we can use CallFrames in an Array<T> by value.
      CallFrame()
      : method(),
        ip(0),
        stackStart(0)
      {}
      
      CallFrame(gc<Method> method, int stackStart)
      : method(method),
        ip(0),
        stackStart(stackStart)
      {}
      
      gc<Method>  method;
      int         ip;
      int         stackStart;
    };
    
    Fiber(VM& vm);
    
    temp<Object> run();
    void call(gc<Method> method, int stackStart);
    
    // Loads a register for the given callframe.
    inline gc<Object> load(const CallFrame& frame, int reg)
    {
      return stack_[frame.stackStart + reg];
    }
    
    // Stores a register for the given callframe.
    inline void store(const CallFrame& frame, int reg, gc<Object> value)
    {
      stack_[frame.stackStart + reg] = value;
    }
    
    gc<Object> loadRegisterOrConstant(const CallFrame& frame, int index);
    
    VM&                 vm_;
    Array<gc<Object> >  stack_;
    Array<CallFrame>    callFrames_;

    NO_COPY(Fiber);
  };
}