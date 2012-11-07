#include <unistd.h>
#include <linux/limits.h>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  void getCoreLibPath(char* path, uint32_t length)
  {
    char relativePath[PATH_MAX];

    int len = readlink("/proc/self/exe", relativePath, PATH_MAX-1);
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
      strncat(relativePath, "/../../..", PATH_MAX);
    }

    // Add library path.
    strncat(relativePath, "/core/core.mag", PATH_MAX);

    // Canonicalize the path.
    realpath(relativePath, path);
  }
}