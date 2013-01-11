#include <unistd.h>

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

  bool File::isOpen() const
  {
    return os_->descriptor != -1;
  }

  void File::close()
  {
    ASSERT(os_->descriptor != -1, "Cannot close a closed file.");
    ::close(os_->descriptor);
    os_->descriptor = -1;
  }
}

