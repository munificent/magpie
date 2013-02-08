#include "uv.h"

#include "Fiber.h"
#include "Module.h"
#include "Object.h"
#include "ObjectIO.h"
#include "Scheduler.h"
#include "VM.h"

namespace magpie
{
  uv_loop_t* Task::loop()
  {
    return fiber_->scheduler().loop_;
  }

  void Task::complete(gc<Object> returnValue)
  {
    // Unlink from the list. We do this before running the fiber so that if
    // the main fiber ends and kills all waiting fibers, it doesn't see this
    // one.
    fiber_->scheduler().tasks_.remove(this);

    // Translate VM NULL to Magpie nothing so that the callback doesn't have
    // to bother looking up the VM to get it.
    if (returnValue.isNull()) returnValue = fiber_->vm().nothing();

    fiber_->storeReturn(returnValue);

    Scheduler& scheduler = fiber_->scheduler();
    scheduler.run(fiber_);

    // We're done!
    delete this;
  }

  Task::Task(gc<Fiber> fiber)
  : fiber_(fiber),
    prev_(NULL),
    next_(NULL)
  {
    fiber->scheduler().add(this);
  }

  void Task::reach()
  {
    fiber_.reach();
  }

  void TaskList::remove(Task* task)
  {
    // Unlink it from its siblings.
    if (task->prev_ != NULL) task->prev_->next_ = task->next_;
    if (task->next_ != NULL) task->next_->prev_ = task->prev_;

    // Handle the ends of the list.
    if (head_ == task) head_ = task->next_;
    if (tail_ == task) tail_ = task->prev_;
  }

  void TaskList::killAll()
  {
    Task* task = head_;
    while (task != NULL)
    {
      task->kill();
      task = task->next_;
    }
    // TODO(bob): Delete Tasks.
  }

  void TaskList::reach()
  {
    Task* task = head_;
    while (task != NULL)
    {
      task->reach();
      task = task->next_;
    }
  }

  void TaskList::add(Task* task)
  {
    // Handle the ends of the list.
    if (head_ == NULL) head_ = task;
    if (tail_ != NULL) tail_->next_ = task;

    // Add it to the end of the list.
    task->prev_ = tail_;
    tail_ = task;
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
            tasks_.killAll();
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
    // TODO(bob): Check status.
    Task* task = static_cast<Task*>(handle->data);

    // Calling sleep() returns nothing.
    task->complete(NULL);
  }

  void Scheduler::sleep(gc<Fiber> fiber, int ms)
  {
    // TODO(bob): We could allocate this on the GC heap and then just reach it
    // as needed.
    uv_timer_t* request = new uv_timer_t;
    HandleTask* task = new HandleTask(fiber,
                                      reinterpret_cast<uv_handle_t*>(request));

    uv_timer_init(task->loop(), request);
    uv_timer_start(request, timerCallback, ms, 0);
  }

  void Scheduler::reach()
  {
    ready_.reach();
    tasks_.reach();
  }

  void Scheduler::add(Task* task)
  {
    tasks_.add(task);
  }

  gc<Fiber> Scheduler::getNext()
  {
    if (ready_.count() == 0) return NULL;
    return ready_.removeAt(0);
  }
}
