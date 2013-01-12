#pragma once

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class VM;

  // When the copying collector has moved an object from one semispace to the
  // other, it leaves this in place of the old one. It points to the new moved
  // object.
  class ForwardingAddress : public Managed
  {
  public:
    ForwardingAddress(Managed* address)
    : Managed(),
      address_(address) {}

    // Gets the forwarding pointer that this object has been replaced with if
    // it has been. Otherwise returns NULL.
    virtual Managed* getForwardingAddress() const { return address_; }

  private:
    Managed* address_;

    NO_COPY(ForwardingAddress);
  };
}
