#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include "MagpieString.h"

namespace magpie
{
  namespace path
  {
    char separator() { return '\\'; }

    gc<String> real(gc<String> path)
    {
      char* absolute[MAX_PATH];
      _fullpath(absolute, path, MAX_PATH);
      return String::create(absolute);
    }

    bool fileExists(gc<String> path)
    {
      ASSERT(false, "Not implemented yet.");
      return false;
    }
  }
}
