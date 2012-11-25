#include "Macros.h"
#include "Environment.h"
#include "Path.h"

namespace magpie
{
  gc<String> locateModule(gc<String> programDir, gc<String> name)
  {
    // TODO(bob): Split module name on ".".
    // Build a relative path from the module name.
    gc<String> relative = String::format("%s.mag", name->cString());

    // Try to find it relative to the program first.
    gc<String> path = path::join(programDir, relative);
    if (path::fileExists(path)) return path;

    // Otherwise, try to find it in the system libraries.
    path = path::join(getCoreLibDir(), relative);
    if (path::fileExists(path)) return path;

    // If we got here, it couldn't be located.
    return NULL;
  }
}