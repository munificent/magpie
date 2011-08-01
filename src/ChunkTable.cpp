#include "ChunkTable.h"

namespace magpie {
  int ChunkTable::add(gc<Chunk> chunk) {
    chunks_.add(chunk);
    return chunks_.count() - 1;
  }
  
  gc<Chunk> ChunkTable::find(int id) {
    return chunks_[id];
  }
}

