#pragma once

#include "Array.h"
#include "Chunk.h"
#include "GC.h"

namespace magpie {
  //### bob: code is never removed from this, even if no longer needed.
  //    eventually, it would be good to ref-count or gc the contents of this.
  // A table of Chunks. The compiler adds compiled code to this so that a chunk
  // can be referenced by ID from bytecode.
  class ChunkTable {
  public:
    // Adds the given chunk to the table if not already present, and returns
    // its ID.
    int add(gc<Chunk> chunk);
    
    // Looks up the chunk with the given ID in the table.
    gc<Chunk> find(int id);
    
  private:
    Array<gc<Chunk> > chunks_;
  };
}

