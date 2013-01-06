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
  
  // Forward-declaration of the platform-specific scheduler data.
  class OSScheduler;

  // The Fiber scheduler.
  class Scheduler
  {
  public:
    Scheduler(VM& vm);
    ~Scheduler();

    void run(Array<Module*> modules);

    // TODO(bob): Get this working with libuv!
    gc<Object> runModule(Module* module);

    // Resumes fiber and continues to run resumable fibers until either the
    // main fiber has ended or all fibers are waiting for events.
    gc<Object> run(gc<Fiber> fiber);

    // Spawns a new Fiber running the given procedure.
    void spawn(gc<FunctionObject> function);
    void add(gc<Fiber> fiber);

    // TODO(bob): Temp?
    void scheduleRead(gc<Fiber> fiber, gc<FileObject> file);

    void sleep(gc<Fiber> fiber, int ms);
    
    void reach();

  private:
    void waitForOSEvents();
    gc<Fiber> getNext();

    VM& vm_;
    OSScheduler* os_;
    uv_loop_t *loop_;

    // Fibers that are not blocked and can run now.
    Array<gc<Fiber> > ready_;

    NO_COPY(Scheduler);
  };
}

