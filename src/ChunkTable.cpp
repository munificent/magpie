#include "ChunkTable.h"

namespace magpie {
  int ChunkTable::add(Ref<Chunk> chunk) {
    chunks_.add(chunk);
    return chunks_.count() - 1;
  }
  
  Ref<Chunk> ChunkTable::find(int id) {
    return chunks_[id];
  }
}

