#pragma once

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

    gc<Object> runModule(Module* module);

    // Spawns a new Fiber running the given procedure.
    void spawn(gc<FunctionObject> function);
    void add(gc<Fiber> fiber);

    // TODO(bob): Temp?
    void scheduleRead(gc<Fiber> fiber, gc<FileObject> file);
    
    void reach();

  private:
    void waitForOSEvents();
    gc<Fiber> getNext();

    VM& vm_;
    OSScheduler* os_;

    // Fibers that are not blocked and can run now.
    Array<gc<Fiber> > ready_;

    NO_COPY(Scheduler);
  };
}

