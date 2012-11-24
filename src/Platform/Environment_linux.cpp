#include <unistd.h>
#include <cstring>
#include <fstream>
#include <linux/limits.h>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  gc<String> getCoreLibPath()
  {
    char* relativePath[PATH_MAX];

    int len = readlink("/proc/self/exe", relativePath, PATH_MAX - 1);
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
    char path[PATH_MAX];
    realpath(relativePath, path);
    return String::create(path);
  }

  // Reads a file from the given path into a String.
  gc<String> readFile(gc<String> path)
  {
    // TODO(bob): Use platform-native API for this?
    using namespace std;

    ifstream stream(path->cString());

    if (stream.fail())
    {
      cerr << "Could not open file '" << path << "'." << endl;
      return gc<String>();
    }

    // From: http://stackoverflow.com/questions/2602013/read-whole-ascii-file-into-c-stdstring.
    string str;

    // Allocate a std::string big enough for the file.
    stream.seekg(0, ios::end);
    str.reserve(stream.tellg());
    stream.seekg(0, ios::beg);

    // Read it in.
    str.assign((istreambuf_iterator<char>(stream)),
               istreambuf_iterator<char>());

    return String::create(str.c_str());
  }
}