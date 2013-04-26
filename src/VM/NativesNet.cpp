#include <sstream>

#include "ObjectNet.h"
#include "NativesNet.h"
#include "VM.h"

namespace magpie
{
  NATIVE(bindNet)
  {
    vm.bindClass("net", CLASS_TCP_LISTENER, "TcpListener");
    return vm.nothing();
  }

  NATIVE(tcpListenerNew)
  {
    return new TcpListenerObject(fiber, asString(args[1]), asInt(args[2]));
  }

  NATIVE(tcpListenerStart)
  {
    gc<TcpListenerObject> listener = asTcpListener(args[0]);
    listener->start(fiber, asFunction(args[1]));

    return vm.nothing();
  }

  NATIVE(tcpListenerStop)
  {
    gc<TcpListenerObject> listener = asTcpListener(args[0]);
    listener->stop();
    return vm.nothing();
  }

  void defineNetNatives(VM& vm)
  {
    DEF_NATIVE(bindNet);
    DEF_NATIVE(tcpListenerNew);
    DEF_NATIVE(tcpListenerStart);
    DEF_NATIVE(tcpListenerStop);
  }
}

