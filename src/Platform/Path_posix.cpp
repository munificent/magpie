#include <stdlib.h>
#include <sys/stat.h>

#include "MagpieString.h"
#include "Path.h"

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
