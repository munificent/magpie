#pragma once

#include <stdint.h>

#include "MagpieString.h"

namespace magpie
{
  // Fills in [path] with the full path to the root directory containing the
  // core library modules.
  gc<String> getCoreLibPath();

  // TODO(bob): Move this into a File module/class.
  // TODO(bob): Better error reporting.
  // Reads the text file at [path] and returns its contents. Returns NULL if
  // the file could not be read.
  gc<String> readFile(gc<String> path);
}
