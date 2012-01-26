#pragma once

#include "Array.h"
#include "GC.h"
#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Method;
  
  class CallFrame
  {
  public:
    // So that we can use CallFrames in an Array<T> by value.
    CallFrame();
    
    CallFrame(gc<Method> method, int stackStart);

    inline gc<Method> method() const { return method_; }
    inline int stackStart() const { return stackStart_; }
    
  private:
    gc<Method>  method_;
    int         instruction_;
    int         stackStart_;
  };
}

