#pragma once

#include "Macros.h"
#include "Token.h"

namespace magpie
{
  class ErrorReporter
  {
  public:
    ErrorReporter()
    : numErrors_(0)
    {}
    
    void error(const SourcePos& pos, const char* format, ...);
    
    int numErrors() const { return numErrors_; }
    
  private:
    int numErrors_;
  };
}
