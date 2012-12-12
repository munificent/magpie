#include "Fiber.h"
#include "Module.h"
#include "Object.h"
#include "Scheduler.h"

namespace magpie
{
  gc<Object> Scheduler::runModule(Module* module)
  {
    /*
     
    Things a fiber can be waiting for:
    - To receive a value on a channel.
      - Need to store channel receiving from.
      - Can resume when another fiber sends a value to that channel.
      - If multiple fibers are blocked receiving on the same channel, a send
        will have to decide which one to send to. FIFO? Random?
    - To finish sending a value on a channel.
      - Needs to store the value being sent, and the channel.
      - Can resume when another fiber does a receive on that channel.
      - If multiple fibers are blocked sending on the same channel, a receive
        will have to decide which one to receive from. FIFO? Random?
    - For an IO operation to complete.
    - For a delay to complete.
    - For the main fiber of the previous module to complete.
    
    loop:
      while there are fibers that can run:
        pick a fiber from the available fibers (how?)
        run it
      wait for os

     */
    gc<FunctionObject> function = FunctionObject::create(module->body());

    gc<Fiber> mainFiber = new Fiber(vm_, *this, function);
    add(mainFiber);

    while (true)
    {
      // Keep running fibers as long as there are ones that are ready.
      gc<Object> value;
      gc<Fiber> fiber;
      while (true)
      {
        if (fiber.isNull()) fiber = getNext();
        if (fiber.isNull()) break;

        FiberResult result = fiber->run(value);
        
        switch (result)
        {
          case FIBER_DONE:
          case FIBER_SUSPEND:
            // Move to the next fiber.
            fiber = NULL;
            break;

          case FIBER_DID_GC:
            // If the fiber returns FIBER_DID_GC, it's still running but it did
            // a GC. Since that moves the fiber, we return back to here so we
            // can invoke run() again at its new location in memory.
            break;

          case FIBER_UNCAUGHT_ERROR:
            // TODO(bob): Kind of hackish.
            // TODO(bob): Give other fibers a chance to handle this.
            // If we got an uncaught error, exit with an error.
            std::cerr << "Uncaught error." << std::endl;
            exit(3);
            break;
        }

        // TODO(bob): Should return value from mainFiber, not whatever fiber
        // was last completed.
        if (mainFiber->isDone()) return value;
      }

      // TODO(bob): Need to handle deadlock case where everything is waiting
      // on channels.
      
      // We aren't done, but everything is suspended, so wait on the OS.
      waitForOSEvents();
    }
  }

  void Scheduler::spawn(gc<FunctionObject> function)
  {
    ready_.add(new Fiber(vm_, *this, function));
  }

  void Scheduler::add(gc<Fiber> fiber)
  {
    ready_.add(fiber);
  }
  
  void Scheduler::reach()
  {
    ready_.reach();
  }

  gc<Fiber> Scheduler::getNext()
  {
    if (ready_.count() == 0) return NULL;
    return ready_.removeAt(0);
  }
}

