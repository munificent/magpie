#pragma once

#include "Array.h"
#include "Callframe.h"
#include "Chunk.h"
#include "GC.h"
#include "Managed.h"
#include "Object.h"

namespace magpie
{
  class VM;

  class Fiber : public Managed
  {
  public:
    Fiber(VM& vm);

    virtual size_t allocSize() const { return sizeof(Fiber); }

    void interpret(gc<Chunk> chunk);

    // TODO(bob): Temp?
    unsigned short addLiteral(gc<Object> value);

  private:
    void run();
    void call(gc<Chunk> chunk, gc<Object> arg);

    VM& vm_;
    Array<gc<CallFrame> > stack_;
    Array<gc<Object> >    literals_;

    NO_COPY(Fiber);
  };
}