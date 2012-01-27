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
    static temp<Method> create();
    
    inline const Array<instruction>& code() const { return code_; }
    
    int addConstant(gc<Object> constant);
    gc<Object> getConstant(int index) const;
    
    void write(instruction code);
    
    int numRegisters() const { return numRegisters_; }
    void setNumRegisters(int numRegisters) { numRegisters_ = numRegisters; }
    // TODO(bob): Implement reach().
    
  private:
    Method()
    : code_(),
      constants_(),
      numRegisters_(0)
    {}
    
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    int numRegisters_;
    
    NO_COPY(Method);
  };

}