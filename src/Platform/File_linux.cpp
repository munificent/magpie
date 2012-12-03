#include "File.h"
#include "File_linux.h"

// TODO(bob): Implement.

namespace magpie
{
  File::File(gc<String> path)
  : path_(path),
    os_(new OSFile())
  {
  }

  File::~File()
  {
    delete os_;
  }
}

