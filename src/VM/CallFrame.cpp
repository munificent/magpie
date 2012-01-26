#include "CallFrame.h"

namespace magpie
{
  CallFrame::CallFrame()
  : method_(),
    instruction_(0),
    stackStart_(0)
  {}
  
  CallFrame::CallFrame(gc<Method> method, int stackStart)
  : method_(method),
    instruction_(0),
    stackStart_(stackStart)
  {}
}
