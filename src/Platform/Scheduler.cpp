#include "uv.h"

#include "Fiber.h"
#include "Module.h"
#include "Object.h"
#include "Scheduler.h"

namespace magpie
{
  WaitingFiber::~WaitingFiber()
  {
    delete handle_;
  }

  WaitingFiber::WaitingFiber(gc<Fiber> fiber, uv_handle_type type,
                             uv_handle_t* handle)
  : fiber_(fiber),
    type_(type),
    handle_(handle),
    prev_(NULL),
    next_(NULL)
  {}

  void WaitingFiberList::add(gc<Fiber> fiber, uv_handle_type type,
                             uv_handle_t* handle)
  {
    WaitingFiber* waiting = new WaitingFiber(fiber, type, handle);

    // Stuff the wait into the handle so we can get it in the callback.
    handle->data = waiting;

    if (head_ == NULL)
    {
      // Only item in the list.
      head_ = waiting;
      tail_ = waiting;
    }
    else
    {
      // Add to the end of the list.
      waiting->prev_ = tail_;
      tail_->next_ = waiting;
    }
  }

  void WaitingFiberList::killAll()
  {
    WaitingFiber* waiting = head_;
    while (waiting != NULL)
    {
      uv_unref(waiting->handle_);
      waiting = waiting->next_;
    }
  }

  void WaitingFiberList::reach()
  {
    WaitingFiber* waiting = head_;
    while (waiting != NULL)
    {
      waiting->fiber_.reach();
      waiting = waiting->next_;
    }
  }

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
          if (fiber->isMain())
          {
            waiting_.killAll();
            return value;
          }

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
    WaitingFiber* waiting = static_cast<WaitingFiber*>(handle->data);
    gc<Fiber> fiber = waiting->fiber();
    Scheduler& scheduler = fiber->scheduler();
    scheduler.run(fiber);
  }

  void Scheduler::sleep(gc<Fiber> fiber, int ms)
  {
    // TODO(bob): We could allocate this on the GC heap and then just reach it
    // as needed.
    uv_timer_t* timer = new uv_timer_t;

    waiting_.add(fiber, UV_TIMER, reinterpret_cast<uv_handle_t*>(timer));
    uv_timer_init(loop_, timer);
    uv_timer_start(timer, timerCallback, ms, 0);
  }
  
  void Scheduler::reach()
  {
    ready_.reach();
    waiting_.reach();
  }

  gc<Fiber> Scheduler::getNext()
  {
    if (ready_.count() == 0) return NULL;
    return ready_.removeAt(0);
  }
}

