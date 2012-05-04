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
    Method()
    : code_(),
      constants_(),
      numRegisters_(0),
      primitive_(NULL)
    {}
    
    // TODO(bob): Get rid of this.
    Method(const Array<instruction>& code,
           const Array<gc<Object> >& constants, int numRegisters)
    : code_(code),
      constants_(constants),
      numRegisters_(numRegisters),
      primitive_(NULL)
    {}
    
    Method(Primitive primitive)
    : code_(),
      constants_(),
      numRegisters_(0),
      primitive_(primitive)
    {}
    
    void setCode(const Array<instruction>& code,
                 int maxRegisters);
    
    inline const Array<instruction>& code() const { return code_; }
    inline Primitive primitive() const { return primitive_; }
    
    int addConstant(gc<Object> constant);
    gc<Object> getConstant(int index) const;
    
    // Adds the given method to this method's list of contained methods. Returns
    // the index of the added method.
    int addMethod(gc<Method> method);
    gc<Method> getMethod(int index) const;
    
    int numRegisters() const { return numRegisters_; }

    void debugTrace() const;
    void debugTrace(instruction ins) const;

    virtual void reach();
    
  private:
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    
    // Methods declared within this method.
    Array<gc<Method> > methods_;
    
    int numRegisters_;
    
    // The primitive function for this method. Will be NULL for non-primitive
    // methods.
    Primitive primitive_;
    
    NO_COPY(Method);
  };

  class MethodScope
  {
  public:
    int declare(gc<String> name);
    void define(int index, gc<Method> method);
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