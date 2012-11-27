#pragma once

#include "Macros.h"

namespace magpie
{
  // Forward-declaration of the platform-specific file data.
  class OSFile;

  // An open file.
  class File
  {
  public:
    File();
    ~File();

  private:
    OSFile* os_;

    NO_COPY(File);
  };
}

