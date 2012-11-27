#include "File.h"

// TODO(bob): Implement.

namespace magpie
{
  class OSFile
  {
  };

  File::File()
  : os_(new OSFile())
  {
  }

  File::~File()
  {
    delete os_;
  }
}

