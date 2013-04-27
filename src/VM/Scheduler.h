#pragma once

#include "uv.h"

#include "Array.h"
#include "Macros.h"

namespace magpie
{
  // suspend stuff:
  class ChannelObject;

  class BufferObject;
  class Fiber;
  class FileObject;
  class FunctionObject;
  class Module;
  class Object;

  // Wraps a Fiber that is waiting for an asynchronous event to complete. This
  // is a manually memory managed doubly linked list.
  class Task
  {
    friend class TaskList;

  public:
    virtual ~Task() {}
    
    gc<Fiber> fiber() { return fiber_; }

    // Gets the main libuv loop this task will run on.
    uv_loop_t* loop();
    
    virtual void kill() = 0;
    
    // Completes the task. Removes it from the list of pending tasks and runs
    // the fiber (and any other fibers that are able to be run).
    //
    // This object will be freed at the end of this call. You cannot use it
    // after this returns!
    void complete(gc<Object> returnValue);

    virtual void reach();

  protected:
    // Creates a new task. Note that instantiating a task implicitly adds it
    // to the scheduler's task list.
    Task(gc<Fiber> fiber);

  private:
    gc<Fiber> fiber_;

    Task* prev_;
    Task* next_;
  };

  // A task using a uv_handle_t.
  class HandleTask : public Task
  {
  public:
    HandleTask(gc<Fiber> fiber, uv_handle_t* handle);
    ~HandleTask();
    virtual void kill();

  private:
    uv_handle_t* handle_;
  };

  // A list of pending asynchronous tasks.
  class TaskList
  {
  public:
    TaskList()
    : head_(NULL),
      tail_(NULL)
    {}
    
    void add(Task* task);

    // Removes [waiting] from this list. Does not free it.
    void remove(Task* task);

    // Cancel all waiting fibers so that the event loop can exit.
    void killAll();

    // Reach all of the waiting fibers so they don't get collected.
    void reach();

  private:
    Task* head_;
    Task* tail_;
  };

  // The Fiber scheduler.
  class Scheduler
  {
    friend class Task;
    
  public:
    Scheduler(VM& vm);

    uv_loop_t* loop() { return loop_; }

    uv_tty_t* tty() { return &tty_; }
    
    void run(Array<Module*> modules);

    // TODO(bob): Get this working with libuv!
    gc<Object> runModule(Module* module);

    // Resumes fiber and continues to run resumable fibers until either the
    // main fiber has ended or all fibers are waiting for events.
    gc<Object> run(gc<Fiber> fiber);

    // Spawns a new Fiber to run the given procedure but does not immediately
    // start it.
    void spawn(gc<FunctionObject> function);

    // Spawns a new Fiber to run the given procedure and immediately runs it.
    // This should only be called from libuv callbacks and not from code already
    // within the main scheduler run loop to prevent re-entrancy.
    void run(gc<FunctionObject> function);

    void add(gc<Fiber> fiber);

    void sleep(gc<Fiber> fiber, int ms);
    void reach();

  private:
    void add(Task* task);

    void waitForOSEvents();
    gc<Fiber> getNext();

    VM& vm_;
    uv_loop_t *loop_;
    uv_tty_t tty_;

    // Fibers that are not blocked and can run now.
    Array<gc<Fiber> > ready_;

    // Fibers that are waiting on an OS event to complete.
    TaskList tasks_;

    NO_COPY(Scheduler);
  };
}

