#pragma once

#include "uv.h"

#include "Common.h"
#include "Memory/Memory.h"
#include "VM/Fiber.h"
#include "VM/Object.h"

namespace magpie
{
  void defineNetNatives(VM& vm);

  class TcpListenerObject : public Object
  {
  public:
    TcpListenerObject(Fiber& fiber, gc<String> address, int port);

    virtual gc<ClassObject> getClass(VM& vm) const;
    virtual gc<String> toString() const;

    void start(Fiber& fiber, gc<FunctionObject> callback);
    void stop();

    void accept();
    gc<StreamObject> takeLastStream();

  private:
    // The scheduler for the VM that owns this listener.
    Scheduler& scheduler_;

    // The function to run on a new fiber when a connection comes in. Will be
    // null if the listener is not currently listening.
    gc<FunctionObject> callback_;

    // TODO(bob): This needs to be allocated on a non-GC heap so it doesn't get
    // moved under libuv when a GC occurs.
    uv_tcp_t server_;

    gc<StreamObject> lastStream_;
  };
}

