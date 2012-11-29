#pragma once

#include "Array.h"
#include "Macros.h"

namespace magpie
{
  class Fiber;
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

    void reach();

  private:
    gc<Fiber> getNext();

    VM& vm_;
    OSScheduler* os_;

    // Fibers that are not blocked and can run now.
    Array<gc<Fiber> > ready_;

    Array<gc<Fiber> > fibers_;

    NO_COPY(Scheduler);
  };
}

