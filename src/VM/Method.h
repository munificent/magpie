#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Macros.h"
#include "Managed.h"
#include "Memory.h"

namespace magpie
{
  class Module;
  class Object;
  
  class Method : public Managed
  {
  public:
    Method()
    : code_(),
      constants_(),
      numSlots_(0)
    {}
        
    void setCode(const Array<instruction>& code,
                 int maxSlots);
    
    inline const Array<instruction>& code() const { return code_; }
    
    int addConstant(gc<Object> constant);
    gc<Object> getConstant(int index) const;
    
    int numSlots() const { return numSlots_; }

    void debugTrace() const;
    void debugTrace(instruction ins) const;

    virtual void reach();
    
  private:
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    
    int numSlots_;
    
    NO_COPY(Method);
  };

  class MethodScope
  {
  public:
    void define(gc<String> name, gc<Method> method);
    
    int find(gc<String> name) const;
    gc<Method> get(int index) const { return methods_[index]; }
    
    void reach();
    
  private:
    Array<gc<Method> > methods_;
    Array<gc<String> > names_;
  };
}