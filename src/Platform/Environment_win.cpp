#include <mach-o/dyld.h>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  void getCoreLibPath(char* path, uint32_t length)
  {
    char relativePath[PATH_MAX];

    // TODO(bob): Move platform-specific stuff out to another file.
    GetModuleFileName(NULL, relativePath, PATH_MAX);
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
      strncat(relativePath, "/..", PATH_MAX);
    }

    // Add library path.
    strncat(relativePath, "/core/core.mag", length);

    // Canonicalize the path.
    _fullpath(path, relativePath, length);
  }
}