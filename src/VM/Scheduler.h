#pragma once

#include "uv.h"

#include "Array.h"
#include "Macros.h"

namespace magpie
{
  // suspend stuff:
  class ChannelObject;
  
  class Fiber;
  class FileObject;
  class FunctionObject;
  class Module;
  class Object;

  // Wraps a Fiber that is waiting for an event to complete. This is a manually
  // memory managed doubly linked list.
  class WaitingFiber
  {
    friend class WaitingFiberList;

  public:
    ~WaitingFiber();
    
    gc<Fiber> fiber() { return fiber_; }
    
  private:
    WaitingFiber(gc<Fiber> fiber, uv_handle_t* handle);
    WaitingFiber(gc<Fiber> fiber, uv_req_t* request);

    gc<Fiber> fiber_;

    uv_handle_t*  handle_;
    uv_req_t*     request_;

    WaitingFiber* prev_;
    WaitingFiber* next_;
  };

  class WaitingFiberList
  {
  public:
    WaitingFiberList()
    : head_(NULL),
      tail_(NULL)
    {}
    
    // Adds [fiber] and the event its waiting on to the list. This will take
    // ownership of [handle]: when the WaitingFiber is freed, it will free the
    // handle itself.
    void add(gc<Fiber> fiber, uv_handle_t* handle);

    // Adds [fiber] and the event its waiting on to the list. This will take
    // ownership of [handle]: when the WaitingFiber is freed, it will free the
    // handle itself.
    void add(gc<Fiber> fiber, uv_req_t* request);

    // Cancel all waiting fibers so that the event loop can exit.
    void killAll();

    // Reach all of the waiting fibers so they don't get collected.
    void reach();

  private:
    void add(WaitingFiber* waiting);

    WaitingFiber* head_;
    WaitingFiber* tail_;
  };

  // The Fiber scheduler.
  class Scheduler
  {
  public:
    Scheduler(VM& vm);

    void run(Array<Module*> modules);

    // TODO(bob): Get this working with libuv!
    gc<Object> runModule(Module* module);

    // Resumes fiber and continues to run resumable fibers until either the
    // main fiber has ended or all fibers are waiting for events.
    gc<Object> run(gc<Fiber> fiber);

    // Spawns a new Fiber running the given procedure.
    void spawn(gc<FunctionObject> function);
    void add(gc<Fiber> fiber);
    
    void sleep(gc<Fiber> fiber, int ms);

    // TODO(bob): Putting these right on Scheduler feels wrong.
    void openFile(gc<Fiber> fiber, gc<String> path);
    void closeFile(gc<Fiber> fiber, gc<FileObject> file);

    void reach();

  private:
    void waitForOSEvents();
    gc<Fiber> getNext();

    VM& vm_;
    uv_loop_t *loop_;

    // Fibers that are not blocked and can run now.
    Array<gc<Fiber> > ready_;

    // Fibers that are waiting on an OS event to complete.
    WaitingFiberList waiting_;

    NO_COPY(Scheduler);
  };
}

