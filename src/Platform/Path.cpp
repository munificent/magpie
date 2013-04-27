#include "Platform/Path.h"

namespace magpie
{
  namespace path
  {
    gc<String> dir(gc<String> path)
    {
      // Find the last directory separator.
      int lastSeparator;
      for (lastSeparator = path->length() - 1; lastSeparator >= 0;
           lastSeparator--)
      {
        if ((*path)[lastSeparator] == separator()) break;
      }

      // If there are no directory separators, just return the original path.
      if (lastSeparator == -1) return path;

      return path->substring(0, lastSeparator);
    }

    gc<String> join(gc<String> a, gc<String> b)
    {
      // TODO(bob): Handle lots of edge cases better here.
      return String::format("%s%c%s", a->cString(), separator(), b->cString());
    }
  }
}
