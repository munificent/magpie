#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Macros.h"
#include "Managed.h"
#include "Memory.h"

namespace magpie
{
  class Object;
  
  typedef gc<Object> (*Primitive)(gc<Object> arg);
  
  class Method : public Managed
  {
  public:
    Method(gc<String> name, const Array<instruction>& code,
           const Array<gc<Object> >& constants, int numRegisters)
    : name_(name),
      code_(code),
      constants_(constants),
      numRegisters_(numRegisters),
      primitive_(NULL)
    {}
    
    Method(gc<String> name, Primitive primitive)
    : name_(name),
      code_(),
      constants_(),
      numRegisters_(0),
      primitive_(primitive)
    {}
    
    gc<String> name() const { return name_; }
    inline const Array<instruction>& code() const { return code_; }
    inline Primitive primitive() const { return primitive_; }
    
    gc<Object> getConstant(int index) const;
    
    int numRegisters() const { return numRegisters_; }

    void debugTrace() const;
    void debugTrace(instruction ins) const;

    virtual void reach();
    
  private:
    gc<String>         name_;
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    int numRegisters_;
    
    // The primitive function for this method. Will be NULL for non-primitive
    // methods.
    Primitive primitive_;
    
    NO_COPY(Method);
  };

  class MethodScope
  {
  public:
    void declare(gc<String> name);
    void define(gc<String> name, gc<Method> method);
    void define(gc<String> name, Primitive primitive);
    
    int find(gc<String> name) const;
    gc<Method> get(int index) const { return methods_[index]; }
    
    gc<Method> findMain() const;
    
    void reach();
    
  private:
    Array<gc<Method> > methods_;
    Array<gc<String> > names_;
  };
}