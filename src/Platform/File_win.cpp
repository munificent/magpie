#include "File.h"
#include "File_win.h"

// TODO(bob): Implement.

namespace magpie
{
  class OSFile
  {
  };

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

