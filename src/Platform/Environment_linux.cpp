#include <unistd.h>
#include <cstring>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  void getCoreLibPath(char* path, uint32_t length)
  {
    char* relativePath = new char[length];

    int len = readlink("/proc/self/exe", relativePath, length-1);
    ASSERT(len != -1, "Executable path too long.");
    relativePath[len] = '\0';

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
    if (strstr(relativePath, "1/out") != 0)
    {
      strncat(relativePath, "/../../..", length);
    }

    // Add library path.
    strncat(relativePath, "/core/core.mag", length);

    // Canonicalize the path.
    realpath(relativePath, path);
    delete[] relativePath;
  }
}