#pragma once

#include "Array.h"
#include "Managed.h"
#include "Memory.h"
#include "Method.h"

namespace magpie
{
  class CatchFrame;
  class FunctionObject;
  class Object;
  class Upvar;
  class VM;

  // The reason Fiber::run() returned.
  enum FiberResult
  {
    // The fiber's entry chunk has completed and the fiber is complete.
    FIBER_DONE,

    // The fiber has been suspended to pass execution to another fiber.
    FIBER_SUSPEND,
    
    // A garbage collection is happened, so the fiber has moved in memory.
    FIBER_DID_GC,
    
    // An error was thrown and not caught by anything, so the fiber has
    // completely unwound.
    FIBER_UNCAUGHT_ERROR
  };

  // When a native function returns, this describes how the return value should
  // be used.
  enum NativeResult
  {
    // A normal return value. It will be the result of the native expression.
    NATIVE_RESULT_RETURN,

    // An error that should be thrown.
    NATIVE_RESULT_THROW,

    // The native has pushed a call frame on the stack, so it doesn't have a
    // return value (yet).
    NATIVE_RESULT_CALL,

    // The native is suspending this fiber. It will be resumed later.
    NATIVE_RESULT_SUSPEND
  };

  class Fiber : public Managed
  {
  public:
    Fiber(VM& vm, gc<FunctionObject> function);
    
    FiberResult run(gc<Object>& result);
    void storeReturn(gc<Object> value);

    virtual void reach();
    virtual void trace(std::ostream& out) const;

  private:
    struct CallFrame
    {
      // So that we can use CallFrames in an Array<T> by value.
      CallFrame()
      : function(),
        ip(0),
        stackStart(0)
      {}
      
      CallFrame(gc<FunctionObject> function, int stackStart)
      : function(function),
        ip(0),
        stackStart(stackStart)
      {}
      
      gc<FunctionObject> function;
      int                ip;
      int                stackStart;
    };
    
    void call(gc<FunctionObject> function, int stackStart);
    
    // Loads a slot for the given callframe.
    inline gc<Object> load(const CallFrame& frame, int slot)
    {
      return stack_[frame.stackStart + slot];
    }
    
    // Stores a slot for the given callframe.
    inline void store(const CallFrame& frame, int slot, gc<Object> value)
    {
      stack_[frame.stackStart + slot] = value;
    }
    
    // Throws the given error object. Returns true if a catch handler was found
    // or false if the error unwound the entire callstack.
    bool throwError(gc<Object> error);

    gc<Object> loadSlotOrConstant(const CallFrame& frame, int index);

    gc<FunctionObject> loadFunction(CallFrame& frame, int chunkSlot);

    // Gets the number of slots that are currently active on the stack.
    int numActiveSlots() const;

    // Closes any open upvars that are now past the end of the stack.
    void closeUpvars();

    gc<Upvar> captureUpvar(int slot);

    static int          nextId_;
    
    VM&                 vm_;
    int                 id_;
    Array<gc<Object> >  stack_;
    Array<CallFrame>    callFrames_;
    gc<CatchFrame>      nearestCatch_;
    gc<Upvar>           openUpvars_;

    NO_COPY(Fiber);
  };

  // A closure: a reference to a variable declared in an outer scope.
  class Upvar : public Managed
  {
  public:
    Upvar()
    {}

    // Gets the value of the variable that the upvar is currently referencing.
    gc<Object> value() { return value_; }

    // Sets the value of the variable the upvar is referencing.
    void setValue(gc<Object> value) { value_ = value; }

  private:
    gc<Object> value_;
  };
    
  // Describes a block containing a "catch" clause that is currently on the
  // stack. When an error is thrown, this is used to jump to the appropriate
  // catch handler(s).
  class CatchFrame : public Managed
  {
  public:
    CatchFrame(gc<CatchFrame> parent, int callFrame, int offset)
    : parent_(parent),
      callFrame_(callFrame),
      offset_(offset)
    {}
    
    void reach();
    
    gc<CatchFrame> parent() const { return parent_; }
    int callFrame() const { return callFrame_; }
    int offset() const { return offset_; }
    
  private:
    // The next enclosing catch. If this catch doesn't handle the error, it
    // will be rethrown to its parent. If the parent is null, then the error
    // is unhandled and the fiber will abort.
    gc<CatchFrame> parent_;
    
    // Index of the CallFrame for the chunk containing this catch.
    int callFrame_;
    
    // The offset of the instruction to jump to in the containing chunk to
    // start executing the catch handler.
    int offset_;
  };
}