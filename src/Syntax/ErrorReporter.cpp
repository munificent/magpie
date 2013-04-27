#include <sstream>
#include <cstdarg>

#include "Syntax/ErrorReporter.h"

namespace magpie
{
  void ErrorReporter::error(gc<SourcePos> pos, const char* format, ...)
  {
    // If we're just waiting for more input, don't show any errors.
    if (needMoreLines_) return;

    numErrors_++;

    // TODO(bob): Hackish. Need to figure out if we want C-style, C++-style or
    // Magpie GC strings.
    char message[512];

    va_list args;
    va_start(args, format);
    vsprintf(message, format, args);
    va_end(args);

    // If we don't have any position information, just show the message.
    if (pos.isNull())
    {
      std::cerr << "Error: " << message << std::endl;
      return;
    }

    std::cerr << "[" << pos->file()->path() << "] Error: " << message << std::endl;

    if (pos->startLine() == pos->endLine())
    {
      // Show the line and highlight the error.
      std::cerr << pos->startLine() << ": "
                << pos->file()->getLine(pos->startLine()) << std::endl;

      // TODO(bob): Lame hack!
      int line = pos->startLine();
      while (line > 0)
      {
        std::cerr << " ";
        line /= 10;
      }

      std::cerr << "  ";
      for (int i = 1; i < pos->endCol(); i++)
      {
        std::cerr << (i < pos->startCol() ? " " : "^");
      }

      std::cerr << std::endl;
    }
    else
    {
      // Show all of the lines.
      for (int i = pos->startLine(); i <= pos->endLine(); i++)
      {
        // TODO(bob): Should pad line number so they all line up.
        std::cerr << i << ": " << pos->file()->getLine(i) << std::endl;
      }
    }
  }

  void ErrorReporter::setNeedMoreLines()
  {
    if (isRepl_) needMoreLines_ = true;
  }
}

