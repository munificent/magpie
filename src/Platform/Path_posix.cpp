// TODO(bob): PATH_MAX isn't actually part of POSIX. Need to do something
// smarter here.
#ifdef __linux__
  #include <linux/limits.h>
#else
#include <limits.h>
#endif
#include <stdlib.h>
#include <sys/stat.h>

#include "Data/String.h"
#include "Platform/Path.h"

namespace magpie
{
  namespace path
  {
    char separator() { return '/'; }

    gc<String> real(gc<String> path)
    {
      char absolute[PATH_MAX];
      realpath(path->cString(), absolute);
      return String::create(absolute);
    }

    bool fileExists(gc<String> path)
    {
      // If we can stat it, it exists.
      struct stat dummy;
      return stat(path->cString(), &dummy) == 0;
    }
  }
}
