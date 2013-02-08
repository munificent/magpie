#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include "MagpieString.h"
#include "Path.h"

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
      DWORD fileAttribs = GetFileAttributes(path->cString());
      return (fileAttribs != INVALID_FILE_ATTRIBUTES) && !(fileAttribs & FILE_ATTRIBUTE_DIRECTORY);
    }
  }
}
