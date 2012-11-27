#pragma once

#include "Array.h"
#include "Macros.h"

namespace magpie
{
  class Fiber;

  // Forward-declaration of the platform-specific scheduler data.
  class OSScheduler;

  // The Fiber scheduler.
  class Scheduler
  {
  public:
    Scheduler();
    ~Scheduler();

    void add(gc<Fiber> fiber);
    void remove(gc<Fiber> fiber);
    gc<Fiber> getNext();

    void reach();

  private:
    OSScheduler* os_;

    Array<gc<Fiber> > fibers_;

    NO_COPY(Scheduler);
  };
}

