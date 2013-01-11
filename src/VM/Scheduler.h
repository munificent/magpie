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

  // Wraps a Fiber that is waiting for an asynchronous event to complete. This
  // is a manually memory managed doubly linked list.
  class Task
  {
    friend class TaskList;

  public:
    virtual ~Task() {}
    
    gc<Fiber> fiber() { return fiber_; }

    virtual void kill() = 0;
    
    // Completes the task. Removes it from the list of pending tasks and runs
    // the fiber (and any other fibers that are able to be run).
    //
    // This object will be freed at the end of this call. You cannot use it
    // after this returns!
    void complete(gc<Object> returnValue);

  protected:
    Task(gc<Fiber> fiber);

  private:
    gc<Fiber> fiber_;

    Task* prev_;
    Task* next_;
  };

  // A task for a file system operation.
  class FSTask : public Task
  {
    friend class TaskList;

  public:
    ~FSTask();

    virtual void kill();

  private:
    FSTask(gc<Fiber> fiber);

    uv_fs_t fs_;
  };

  // A task using a uv_handle_t.
  class HandleTask : public Task
  {
    friend class TaskList;

  public:
    ~HandleTask();
    virtual void kill();

  private:
    HandleTask(gc<Fiber> fiber, uv_handle_t* handle);

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
    
    // Create a new file system task that is blocking [fiber].
    uv_fs_t* createFS(gc<Fiber> fiber);

    // Create a new pipe task that is blocking [fiber].
    uv_pipe_t* createPipe(gc<Fiber> fiber);

    // Create a new timer task that is blocking [fiber].
    uv_timer_t* createTimer(gc<Fiber> fiber);

    // Removes [waiting] from this list. Does not free it.
    void remove(Task* task);

    // Cancel all waiting fibers so that the event loop can exit.
    void killAll();

    // Reach all of the waiting fibers so they don't get collected.
    void reach();

  private:
    void add(Task* task);

    Task* head_;
    Task* tail_;
  };

  // The Fiber scheduler.
  class Scheduler
  {
    friend class Task;
    
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
    void read(gc<Fiber>, gc<FileObject> file);
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
    TaskList tasks_;

    NO_COPY(Scheduler);
  };
}

