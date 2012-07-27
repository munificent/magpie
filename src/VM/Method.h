#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Macros.h"
#include "Managed.h"
#include "Memory.h"

namespace magpie
{
  class DefExpr;
  class Module;
  class Object;
  class Pattern;
  class VM;
  
  // A compiled chunk of bytecode that can be executed by a Fiber.
  class Chunk : public Managed
  {
  public:
    Chunk()
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
    
    NO_COPY(Chunk);
  };
  
  // Intermediate representation of a single method in a multimethod. This is
  // produced during module compilation and "halfway" compiles the method to
  // bytecode. The final compilation to bytecode occurs once all methods for a
  // multimethod are known. The Expr for the body here should already be
  // resolved.
  class Method : public Managed
  {
  public:
    Method(Module* module, gc<DefExpr> def)
    : module_(module),
      def_(def)
    {}
    
    Module* module() { return module_; }
    gc<DefExpr> def() { return def_; }
    
    // TODO(bob): reach().
    
  private:
    Module* module_;
    gc<DefExpr> def_;
  };
  
  class Multimethod : public Managed
  {
  public:
    Multimethod(gc<String> signature);
    
    gc<String> signature() { return signature_; }
    gc<Chunk> getChunk(VM& vm);
    
    void addMethod(gc<Method> method);
    
    // TODO(bob): For now, we can just compile a single method to bytecode.
    // This is just until we get everything working with the new interpreter-
    // style method definitions (where "def" expressions are expressions).
    gc<Method> hackGetMethod();
    
  private:
    // TODO(bob): reach().
    
    gc<String> signature_;
    gc<Chunk> chunk_;
    Array<gc<Method> > methods_;
  };
}