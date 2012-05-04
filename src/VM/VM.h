#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

namespace magpie
{
  class Module;
  
  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource
  {
  public:
    VM();

    void loadModule(Module* module);
    
    virtual void reachRoots();

    Fiber& fiber() { return *fiber_; }

    // The globally available top-level methods.
    MethodScope& methods() { return methods_; }
    
    inline gc<Object> nothing() const { return nothing_; }
    
    inline gc<Object> getBool(bool value) const
    {
      return value ? true_ : false_;
    }
    
    inline gc<Object> getBuiltIn(int value) const
    {
      switch (value) {
        case 0: return false_;
        case 1: return true_;
        case 2: return nothing_;
      }
      
      ASSERT(false, "Unknown built-in ID.");
    }
    
  private:
    Array<Module*> modules_;
    MethodScope methods_;
    gc<Fiber> fiber_;
    
    gc<Object> true_;
    gc<Object> false_;
    gc<Object> nothing_;
    
    NO_COPY(VM);
  };
}

