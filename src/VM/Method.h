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
    static temp<Method> create(gc<String> name,
                               const Array<instruction>& code,
                               const Array<gc<Object> >& constants,
                               int numRegisters);
    
    gc<String> name() const { return name_; }
    inline const Array<instruction>& code() const { return code_; }
    
    gc<Object> getConstant(int index) const;
    
    int numRegisters() const { return numRegisters_; }

    void debugTrace() const;
    void debugTrace(instruction ins) const;

    // TODO(bob): Implement reach().
    
  private:
    Method(gc<String> name, const Array<instruction>& code,
           const Array<gc<Object> >& constants, int numRegisters)
    : name_(name),
      code_(code),
      constants_(constants),
      numRegisters_(numRegisters)
    {}
    
    gc<String>         name_;
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    int numRegisters_;
    
    NO_COPY(Method);
  };

  // TODO(bob): Move to separate file.
  class MethodScope
  {
  public:
    void declare(gc<String> name);
    void define(gc<String> name, gc<Method> method);
    
    int find(gc<String> name) const;
    gc<Method> get(int index) const { return methods_[index]; }
    
    gc<Method> findMain() const;
    
  private:
    Array<gc<Method> > methods_;
    Array<gc<String> > names_;
  };
}