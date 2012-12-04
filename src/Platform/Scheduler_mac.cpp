#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#include <fcntl.h>

#include "Fiber.h"
#include "File.h"
#include "File_mac.h"
#include "Object.h"
#include "Scheduler.h"

namespace magpie
{
  class OSScheduler
  {
  public:
    // kqueue ID returned by kqueue().
    int queue;
  };

  Scheduler::Scheduler(VM& vm)
  : vm_(vm),
    os_(new OSScheduler())
  {
    os_->queue = kqueue();
    if (os_->queue == -1)
    {
      std::cerr << "Could not create kqueue." << std::endl;
      exit(1);
    }
  }

  void Scheduler::scheduleRead(gc<Fiber> fiber, gc<FileObject> fileObj)
  {
    File& file = fileObj->file();

    struct kevent change;
    EV_SET(&change, file.os()->descriptor, EVFILT_READ,
           EV_ADD,
           0,
           0,
           static_cast<void*>(&(*fiber)));
    int result = kevent(os_->queue,
           &change, 1, // Add one change to the queue.
           NULL, 0,    // Don't read any events.
           NULL);      // No timeout.
    // TODO(bob): Better error handling.
    if (result == -1)
    {
      perror("Could not schedule event.");
    }

    fiber->suspend(new FileReadSuspension(fileObj));
  }

  void Scheduler::waitForOSEvents()
  {
    struct kevent event;

    // TODO(bob): Handle timeouts.
    int numEvents = kevent(os_->queue,
                           NULL, 0,   // No changes to the queue.
                           &event, 1, // Read one event.
                           NULL);     // No timeout.

    if (numEvents == 1)
    {
      // TODO(bob): Assumes it's a read event. Handle other event types.
      gc<Fiber> fiber = static_cast<Fiber*>(event.udata);

      // TODO(bob): Nasty. Do something cleaner to downcast here.
      gc<FileReadSuspension> suspension = static_cast<FileReadSuspension*>(
          &(*fiber->ready()));

      char* buffer = new char[event.data + 1];
      int bytesRead = read(suspension->file()->file().os()->descriptor, buffer,
                           event.data);
      // TODO(bob): Handle error.
      buffer[bytesRead] = '\0';

      // TODO(bob): Hack! Assumes file is a text file. Need to handle encodings
      // and binary data.
      gc<String> text = String::create(buffer);
      fiber->storeReturn(new StringObject(text));

      delete [] buffer;
    }
    else if (numEvents == 0)
    {
      // TODO(bob): Handle this. Timeout?
      ASSERT(false, "Not implemented.");
    }
    else
    {
      // TODO(bob): Cleaner fatal error-handling code.
      std::cerr << "Error reading from kqueue." << std::endl;
      exit(1);
    }
  }

  Scheduler::~Scheduler()
  {
    delete os_;
  }
}

