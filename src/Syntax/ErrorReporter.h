#pragma once

#include "Macros.h"
#include "Token.h"

namespace magpie
{
  class ErrorReporter
  {
  public:
    ErrorReporter(bool isRepl = false)
    : isRepl_(isRepl),
      numErrors_(0),
      needMoreLines_(false)
    {}
    
    void error(const SourcePos& pos, const char* format, ...);
    void setNeedMoreLines();
    
    int numErrors() const { return numErrors_; }
    bool needMoreLines() const { return needMoreLines_; }
    
  private:
    bool isRepl_;
    int numErrors_;
    bool needMoreLines_;
  };
}
