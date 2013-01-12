#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "MagpieString.h"

namespace magpie
{
  namespace path
  {
    char separator() { return '\\'; }

    gc<String> real(gc<String> path)
    {
      char absolute[MAX_PATH];
      _fullpath(absolute, path->cString(), MAX_PATH);
      return String::create(absolute);
    }

    bool fileExists(gc<String> path)
    {
      // If we can stat it, it exists.
      struct _stat64 dummy;
      return _stat64(path->cString(), &dummy) == 0;
    }
  }
}
