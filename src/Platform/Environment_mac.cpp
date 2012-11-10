#include <mach-o/dyld.h>
#include <cstring>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  void getCoreLibPath(char* path, uint32_t length)
  {
    char* relativePath = new char[length];

    uint32_t size = length;
    int result = _NSGetExecutablePath(relativePath, &size);
    ASSERT(result == 0, "Executable path too long.");

    // Cut off file name from path
    char* lastSep = NULL;
    for (char* c = relativePath; *c != '\0'; c++)
    {
      if (*c == '/')
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
    if (strstr(relativePath, "build/Debug") != 0 ||
        strstr(relativePath, "build/Release") != 0)
    {
      strncat(relativePath, "/../..", length);
    }

    // Add library path.
    strncat(relativePath, "/core/core.mag", length);

    // Canonicalize the path.
    realpath(relativePath, path);
    delete[] relativePath;
  }
}