#include <sstream>
#include <cstdarg>

#include "ErrorReporter.h"

namespace magpie
{
  void ErrorReporter::error(gc<SourcePos> pos, const char* format, ...)
  {
    // If we're just waiting for more input, don't show any errors.
    if (needMoreLines_) return;

    // TODO(bob): Hackish. Need to figure out if we want C-style, C++-style or
    // Magpie GC strings.
    char message[512];

    va_list args;
    va_start(args, format);
    vsprintf(message, format, args);
    va_end(args);

    std::cerr << "[" << pos->file() << " line "
        << pos->startLine() << " col "
        << pos->startCol() << "] Error: " << message << std::endl;

    numErrors_++;
  }

  void ErrorReporter::setNeedMoreLines()
  {
    if (isRepl_) needMoreLines_ = true;
  }
}

