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

  void Scheduler::scheduleRead(gc<Fiber> fiber, gc<FileObject> fileObj)
  {
    ASSERT(false, "Not implemented yet.");
  }
  
  void Scheduler::waitForOSEvents()
  {
    ASSERT(false, "Not implemented yet.");
  }
}

