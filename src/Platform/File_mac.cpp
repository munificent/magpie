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
}

