#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#include <fcntl.h>

#include "Fiber.h"
#include "Scheduler.h"

namespace magpie
{
  class OSScheduler
  {
  public:
    // kqueue ID returned by kqueue().
    int queue;
  };

  Scheduler::Scheduler()
  : os_(new OSScheduler())
  {
    os_->queue = kqueue();
    if (os_->queue == -1)
    {
      std::cerr << "Could not create kqueue." << std::endl;
      exit(1);
    }
  }

  Scheduler::~Scheduler()
  {
    delete os_;
  }

  /*
   int queue = kqueue();
   if (queue == -1)
   {
   std::cerr << "Couldn't create kqueue." << std::endl;
   return 1;
   }

   int file = open("/Users/rnystrom/dev/magpie/apple.txt", O_RDONLY);
   if (file == -1)
   {
   std::cerr << "Couldn't open file." << std::endl;
   return 1;
   }

   struct kevent change;
   EV_SET(&change, file, EVFILT_READ,
   EV_ADD,
   0,
   0, 0);

   for (;;)
   {
   struct kevent event;
   int numEvents = kevent(queue, &change, 1, &event, 1, NULL);
   if (numEvents == -1)
   {
   perror("kevent");
   }

   if (numEvents == 0) continue;

   char buffer[4096];
   int bytesRead = read(file, buffer, sizeof(buffer));
   printf("%.*s", bytesRead, buffer);
   }

   close(queue);
   close(file);
   return EXIT_SUCCESS;
   */
}

