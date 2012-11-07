#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstring>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  void getCoreLibPath(char* path, uint32_t length)
  {
    char* relativePath = new char[length];

    GetModuleFileName(NULL, relativePath, length);
    ASSERT(GetLastError() != ERROR_INSUFFICIENT_BUFFER, "Executable path too long.")

    // Cut off file name from path
    char* lastSep = NULL;
    for (char* c = relativePath; *c != '\0'; c++)
    {
      if (*c == '/' || *c == '\\')
      {
        lastSep = c;
      }
    }

    if (lastSep != NULL)
    {
      *lastSep = '\0';
    }

    // Find the magpie main directory relative to the executable.
    // TODO(bob): Hack. Try to work from the build directory too.
    if (strstr(relativePath, "Debug") != 0 ||
        strstr(relativePath, "Release") != 0)
    {
      strncat(relativePath, "/..", length);
    }

    // Add library path.
    strncat(relativePath, "/core/core.mag", length);

    // Canonicalize the path.
    _fullpath(path, relativePath, length);
    delete[] relativePath;
  }
}