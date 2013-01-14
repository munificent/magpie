#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <shlwapi.h>

#pragma comment(lib, "Shlwapi")

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
      return PathFileExists(path->cString()) == 1;
    }
  }
}
