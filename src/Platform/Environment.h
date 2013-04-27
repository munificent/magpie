#pragma once

#include "Data/String.h"

namespace magpie
{
  // TODO(bob): Need to decide what should be here and what should be in path.h.

  // Gets the full path to the root directory containing the core library
  // modules.
  gc<String> getCoreLibDir();

  // TODO(bob): Move this into a File module/class.
  // TODO(bob): Better error reporting.
  // Reads the text file at [path] and returns its contents. Returns NULL if
  // the file could not be read.
  gc<String> readFile(gc<String> path);

  // Given a module name, like "foo.bar" returns a path to a Magpie source file
  // to load for that module. Returns NULL if no matching module could be found.
  gc<String> locateModule(gc<String> programDir, gc<String> name);
}
