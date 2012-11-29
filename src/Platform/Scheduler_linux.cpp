#include "Fiber.h"
#include "Scheduler.h"

// TODO(bob): Implement using epoll.

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

