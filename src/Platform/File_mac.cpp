#include <fcntl.h>

#include "File.h"
#include "File_mac.h"

// TODO(bob): Implement.

namespace magpie
{
  File::File(gc<String> path)
  : path_(path),
    os_(new OSFile())
  {
    // TODO(bob): Allow other flags.
    // TODO(bob): Handle errors.
    // TODO(bob): Open lazily on first use?
    os_->descriptor = open(path->cString(), O_RDONLY);
    // TODO(bob): Handle returning -1.
    if (os_->descriptor == -1)
    {
      perror("Could not open file.");
      std::cerr << path << std::endl;
    }
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
    // TODO(bob): Handle error.
    os_->descriptor = -1;
  }
}

