#pragma once

#include <iostream>
#include "uv.h"

#include "Macros.h"
#include "Managed.h"
#include "Object.h"
#include "Scheduler.h"

namespace magpie
{
  class TcpListenerObject;

  // Unsafe downcasting functions. These must *only* be called after the object
  // has been verified as being the right type.
  gc<TcpListenerObject> asTcpListener(gc<Object> obj);

  class TcpListenerObject : public Object
  {
  public:
    TcpListenerObject(Fiber& fiber, gc<String> address, int port);

    virtual gc<ClassObject> getClass(VM& vm) const;
    virtual gc<String> toString() const;

    void start(Fiber& fiber, gc<FunctionObject> callback);
    void stop();

    void accept();

  private:
    // The scheduler for the VM that owns this listener.
    Scheduler& scheduler_;

    // The function to run on a new fiber when a connection comes in. Will be
    // null if the listener is not currently listening.
    gc<FunctionObject> callback_;

    // TODO(bob): This needs to be allocated on a non-GC heap so it doesn't get
    // moved under libuv when a GC occurs.
    uv_tcp_t server_;
  };
}
