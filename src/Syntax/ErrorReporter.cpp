#include <sstream>

#include "ErrorReporter.h"

namespace magpie
{
  void ErrorReporter::error(const SourcePos& pos, const char* format, ...)
  {
    // TODO(bob): Hackish. Need to figure out if we want C-style, C++-style or
    // Magpie GC strings.
    char message[512];
    
    va_list args;
    va_start(args, format);
    vsprintf(message, format, args);
    va_end(args);

    std::cout << "[" << pos.file() << " line "
        << pos.startLine() << " col "
        << pos.startCol() << "] Error: " << message << std::endl;

    numErrors_++;
  }
}

