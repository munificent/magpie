#include "Fiber.h"
#include "Scheduler.h"

namespace magpie
{
  void Scheduler::add(gc<Fiber> fiber)
  {
    fibers_.add(fiber);
  }

  gc<Fiber> Scheduler::getNext()
  {
    if (fibers_.count() == 0) return NULL;
    
    // TODO(bob): Hack temp!
    return fibers_.removeAt(0);
  }

  void Scheduler::reach()
  {
    fibers_.reach();
  }
}

