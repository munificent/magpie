#include "CallFrame.h"

namespace magpie {
  
  CallFrame::CallFrame(gc<Chunk> chunk)
  : Managed(),
    chunk_(chunk),
    instruction_(0),
    registers_(chunk->getNumRegisters()) {

    // Create the registers.
    for (int i = 0; i < chunk_->getNumRegisters(); i++) {
      // TODO(bob): Should this be a nothing object?
      registers_.add(gc<Object>());
    }
  }
}
