#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstring>
#include <fstream>

#include "Macros.h"
#include "Environment.h"

namespace magpie
{
  gc<String> getCoreLibDir()
  {
    char relativePath[MAX_PATH];

    GetModuleFileName(NULL, relativePath, MAX_PATH);
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
      strncat(relativePath, "/..", MAX_PATH);
    }

    // Add the core library directory.
    strncat(relativePath, "/core", MAX_PATH);

    // Canonicalize the path.
    char path[MAX_PATH];
    _fullpath(path, relativePath, MAX_PATH);
    return String::create(path);
  }

  // Reads a file from the given path into a String.
  gc<String> readFile(gc<String> path)
  {
    // TODO(bob): Use platform-native API for this.
    using namespace std;

    ifstream stream(path->cString());

    if (stream.fail()) return gc<String>();

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