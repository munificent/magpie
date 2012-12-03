#pragma once

#include "Macros.h"
#include "MagpieString.h"
#include "Memory.h"

namespace magpie
{
  // Forward-declaration of the platform-specific file data.
  class OSFile;

  // An open file.
  class File
  {
  public:
    File(gc<String> path);
    ~File();

    // Gets the platform-specific file data.
    OSFile* os() { return os_; }

    void read();
    
  private:
    // TODO(bob): Using a gc pointer inside a non-GC type is a bit weird.
    // Need to figure out how to handle this.
    gc<String> path_;
    OSFile* os_;

    NO_COPY(File);
  };
}

