#include <sstream>
#include <fcntl.h>

#include "Array.h"
#include "ObjectNet.h"
#include "VM.h"

namespace magpie
{
  gc<TcpListenerObject> asTcpListener(gc<Object> obj)
  {
    return static_cast<TcpListenerObject*>(&(*obj));
  }

  TcpListenerObject::TcpListenerObject(Fiber& fiber, gc<String> address,
                                       int port)
  : scheduler_(fiber.scheduler()),
    callback_()
  {
    uv_tcp_init(fiber.scheduler().loop(), &server_);
    server_.data = this;

    struct sockaddr_in bindAddr = uv_ip4_addr(address->cString(), port);
    uv_tcp_bind(&server_, bindAddr);
  }

  gc<ClassObject> TcpListenerObject::getClass(VM& vm) const
  {
    return vm.getClass(CLASS_TCP_LISTENER);
  }

  gc<String> TcpListenerObject::toString() const
  {
    // TODO(bob): Show address and port?
    return String::create("[tcp listener]");
  }

  static void tcpListenCallback(uv_stream_t* server, int status) {
    if (status == -1) {
      // TODO(bob): Handle error.
      return;
    }

    // TODO(bob): Pass in connection info.
    reinterpret_cast<TcpListenerObject*>(server->data)->accept();
  }

  void TcpListenerObject::start(Fiber& fiber, gc<FunctionObject> callback)
  {
    // TODO(bob): Should check that we aren't already listening.
    callback_ = callback;
    
    // TODO(bob): Make backlog queue configurable.
    int result = uv_listen(reinterpret_cast<uv_stream_t*>(&server_), 128,
                           tcpListenCallback);

    // TODO(bob): Throw error.
    if (result != 0) {
      std::cerr << "Listen error " /*<< uv_err_name(uv_last_error(hack))*/ << std::endl;
    }
  }

  void TcpListenerObject::stop()
  {
    ASSERT(!callback_.isNull(), "Cannot stop when not listening.");
    // TODO(bob): Need to make sure we are currently started (do actual check
    // and handle it, not just assert).

    callback_ = NULL;
    uv_unref(reinterpret_cast<uv_handle_t*>(&server_));
  }

  void TcpListenerObject::accept()
  {
    ASSERT(!callback_.isNull(), "Cannot accept when not listening.");
    
    // TODO(bob): Manage this memory (but not on the GC heap since that can get
    // moved out from under libuv.
    uv_tcp_t *client = reinterpret_cast<uv_tcp_t*>(malloc(sizeof(uv_tcp_t)));
    uv_tcp_init(scheduler_.loop(), client);

    if (uv_accept((uv_stream_t*) &server_, (uv_stream_t*) client) == 0)
    {
      // Spin up a fiber to handle the connection.
      scheduler_.run(callback_);

      // TODO(bob): Create stream and pass to callback.
      //uv_read_start((uv_stream_t*) client, alloc_buffer, echo_read);
    }
    else
    {
      uv_close(reinterpret_cast<uv_handle_t*>(client), NULL);
      std::cout << "Closed :(" << std::endl;
    }
  }
}
