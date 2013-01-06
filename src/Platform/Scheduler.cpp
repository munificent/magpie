#include "uv.h"

#include "Fiber.h"
#include "Module.h"
#include "Object.h"
#include "Scheduler.h"

namespace magpie
{
  void Scheduler::run(Array<Module*> modules)
  {
    // Queue up fibers for each module body.
    gc<Fiber> moduleFiber;
    for (int i = modules.count() - 1; i >= 0; i--)
    {
      gc<FunctionObject> function = FunctionObject::create(modules[i]->body());
      moduleFiber = new Fiber(vm_, *this, function, moduleFiber);

      if (i == modules.count() - 1) moduleFiber->setAsMain();
    }

    // Initialize the event loop. This way modules can schedule events during
    // their initialization.
    loop_ = uv_loop_new();

    // Start running the first module.
    run(moduleFiber);

    // Now that all of the module initialization is done (or suspended on
    // events), start the event loop.
    uv_run(loop_);
  }

  gc<Object> Scheduler::runModule(Module* module)
  {
    gc<FunctionObject> function = FunctionObject::create(module->body());
    return run(new Fiber(vm_, *this, function, NULL));
  }
  
  gc<Object> Scheduler::run(gc<Fiber> fiber)
  {
    gc<Object> value;

    // Keep running fibers as long as there are ones that are ready.
    // TODO(bob): Lots of copy/paste here with runModule(). Unify.
    while (!fiber.isNull())
    {
      FiberResult result = fiber->run(value);

      switch (result)
      {
        case FIBER_DONE:
          // If the main module has completed, stop.
          if (fiber->isMain()) return value;

          // Advance to the successor if it has one, otherwise try to unsuspend
          // something else.
          fiber = fiber->successor();
          if (fiber.isNull()) fiber = getNext();
          break;

        case FIBER_SUSPEND:
          // Try to move on to the next fiber.
          fiber = getNext();
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
    }

    // TODO(bob): Should return value from first fiber, not whatever fiber
    // was last completed.
    return value;
  }
  
  void Scheduler::spawn(gc<FunctionObject> function)
  {
    ready_.add(new Fiber(vm_, *this, function, NULL));
  }

  void Scheduler::add(gc<Fiber> fiber)
  {
    ready_.add(fiber);
  }

  static void timerCallback(uv_timer_t* handle, int status)
  {
    // TODO(bob): Check status?
    gc<Fiber> fiber = static_cast<Fiber*>(handle->data);

    Scheduler& scheduler = fiber->scheduler();
    scheduler.run(fiber);
  }

  void Scheduler::sleep(gc<Fiber> fiber, int ms)
  {
    // TODO(bob): Manage this memory!
    uv_timer_t* timer_req = new uv_timer_t;

    // TODO(bob): Need to hang onto this fiber somewhere the GC can find it.
    // Otherwise a GC while this fiber is sleeping will collect it.
    timer_req->data = &*fiber;
    
    uv_timer_init(loop_, timer_req);
    uv_timer_start(timer_req, timerCallback, ms, 0);
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

