#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "GC.h"
#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Method : public Managed
  {
  public:
    static temp<Method> create();
    
    inline const Array<instruction>& code() const { return code_; }
    
    int addConstant(gc<Managed> constant);
    void write(instruction code);
    
    // TODO(bob): Implement reach().
    
  private:
    Method()
    : code_() {}
    
    Array<instruction> code_;
    Array<gc<Managed> > constants_;

    NO_COPY(Method);
  };

}