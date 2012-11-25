#pragma once

#include "MagpieString.h"

namespace magpie
{
  namespace path
  {
    // Get the path separator character for the host platform.
    char separator();

    // Joins two path parts together.
    gc<String> join(gc<String> a, gc<String> b);

    // Returns the directory portion of [path]. Example:
    //
    //     path/to/file.txt -> path/to
    gc<String> dir(gc<String> path);
    
    // Canonicalizes [path] and returns the full absolute path relative to the
    // current working directory.
    gc<String> real(gc<String> path);

    // Returns true if there is a file at [path].
    bool fileExists(gc<String> path);
  }
}
