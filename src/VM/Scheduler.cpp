#include "uv.h"

#include "Fiber.h"
#include "Module.h"
#include "Object.h"
#include "Scheduler.h"
#include "VM.h"

namespace magpie
{
  WaitingFiber::~WaitingFiber()
  {
    delete handle_;
  }

  WaitingFiber::WaitingFiber(gc<Fiber> fiber, uv_handle_t* handle)
  : fiber_(fiber),
    handle_(handle),
    request_(NULL),
    prev_(NULL),
    next_(NULL)
  {}

  WaitingFiber::WaitingFiber(gc<Fiber> fiber, uv_req_t* request)
  : fiber_(fiber),
    handle_(NULL),
    request_(request),
    prev_(NULL),
    next_(NULL)
  {}

  void WaitingFiberList::add(gc<Fiber> fiber, uv_handle_t* handle)
  {
    WaitingFiber* waiting = new WaitingFiber(fiber, handle);

    // Stuff the wait into the handle so we can get it in the callback.
    handle->data = waiting;

    add(waiting);

  }

  void WaitingFiberList::add(gc<Fiber> fiber, uv_req_t* request)
  {
    WaitingFiber* waiting = new WaitingFiber(fiber, request);

    // Stuff the wait into the request so we can get it in the callback.
    request->data = waiting;

    add(waiting);
  }

  void WaitingFiberList::killAll()
  {
    WaitingFiber* waiting = head_;
    while (waiting != NULL)
    {
      if (waiting->handle_ != NULL) uv_unref(waiting->handle_);
      if (waiting->request_ != NULL) uv_cancel(waiting->request_);
      
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

  void WaitingFiberList::add(WaitingFiber* waiting)
  {
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

  Scheduler::Scheduler(VM& vm)
  : vm_(vm)
  {}
  
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

    // TODO(bob): Need to remove WaitingFiber from list.
    
    // Calling sleep() returns nothing.
    fiber->storeReturn(fiber->vm().nothing());

    scheduler.run(fiber);
  }

  void Scheduler::sleep(gc<Fiber> fiber, int ms)
  {
    // TODO(bob): We could allocate this on the GC heap and then just reach it
    // as needed.
    uv_timer_t* request = new uv_timer_t;

    waiting_.add(fiber, reinterpret_cast<uv_handle_t*>(request));
    uv_timer_init(loop_, request);
    uv_timer_start(request, timerCallback, ms, 0);
  }

  static void openFileCallback(uv_fs_t* handle)
  {
    WaitingFiber* waiting = static_cast<WaitingFiber*>(handle->data);
    gc<Fiber> fiber = waiting->fiber();
    Scheduler& scheduler = fiber->scheduler();

    // Create and return the file object.
    fiber->storeReturn(new FileObject(handle->file));

    scheduler.run(fiber);
  }
  
  void Scheduler::openFile(gc<Fiber> fiber, gc<String> path)
  {
    // TODO(bob): We could allocate this on the GC heap and then just reach it
    // as needed.
    uv_fs_t* request = new uv_fs_t;

    waiting_.add(fiber, reinterpret_cast<uv_req_t*>(request));

    // TODO(bob): Make this configurable.
    int flags = O_RDONLY;
    // TODO(bob): Make this configurable when creating a file.
    int mode = 0;
    uv_fs_open(loop_, request, path->cString(), flags, mode, openFileCallback);
  }

  static void closeFileCallback(uv_fs_t* handle)
  {
    WaitingFiber* waiting = static_cast<WaitingFiber*>(handle->data);
    gc<Fiber> fiber = waiting->fiber();
    Scheduler& scheduler = fiber->scheduler();

    // Close returns nothing.
    fiber->storeReturn(fiber->vm().nothing());

    scheduler.run(fiber);
  }
  
  void Scheduler::closeFile(gc<Fiber> fiber, gc<FileObject> file)
  {
    // Mark the file closed immediately so other fibers can't try to use it.
    file->close();

    // TODO(bob): We could allocate this on the GC heap and then just reach it
    // as needed.
    uv_fs_t* request = new uv_fs_t;

    waiting_.add(fiber, reinterpret_cast<uv_req_t*>(request));
    
    uv_fs_close(loop_, request, file->file(), closeFileCallback);
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

