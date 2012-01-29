#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Macros.h"
#include "Managed.h"
#include "Memory.h"

namespace magpie
{
  class Object;
  
  class Method : public Managed
  {
  public:
    static temp<Method> create(const Array<instruction>& code,
                               const Array<gc<Object> >& constants,
                               int numRegisters);
    
    inline const Array<instruction>& code() const { return code_; }
    
    gc<Object> getConstant(int index) const;
    
    int numRegisters() const { return numRegisters_; }

    // TODO(bob): Implement reach().
    
  private:
    Method(const Array<instruction>& code, const Array<gc<Object> >& constants,
           int numRegisters)
    : code_(code),
      constants_(constants),
      numRegisters_(numRegisters)
    {}
    
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    int numRegisters_;
    
    NO_COPY(Method);
  };

}