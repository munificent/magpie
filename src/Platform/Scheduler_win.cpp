#include "Fiber.h"
#include "Scheduler.h"

// TODO(bob): Implement using... a thread pool?

namespace magpie
{
  class OSScheduler
  {
  };

  Scheduler::Scheduler(VM& vm)
  : vm_(vm),
    os_(new OSScheduler())
  {
  }

  Scheduler::~Scheduler()
  {
    delete os_;
  }
}

