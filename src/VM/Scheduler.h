#pragma once

#include "Array.h"
#include "Macros.h"

namespace magpie
{
  class Fiber;
  
  // The Fiber scheduler.
  class Scheduler
  {
  public:
    Scheduler() {}

    void add(gc<Fiber> fiber);
    void remove(gc<Fiber> fiber);
    gc<Fiber> getNext();

    void reach();

  private:
    Array<gc<Fiber> > fibers_;

    NO_COPY(Scheduler);
  };
}

