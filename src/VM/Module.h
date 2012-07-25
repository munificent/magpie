#pragma once

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Chunk;
  class Object;
  
  // A module is a single file of compiled Magpie code.
  class Module
  {
  public:
    Module()
    : body_(),
      imports_(),
      variables_(),
      variableNames_()
    {}
    
    void reach();
    
    void bindBody(gc<Chunk> body);
    gc<Chunk> body() const { return body_; }
    
    Array<Module*>& imports() { return imports_; }
    const Array<Module*>& imports() const { return imports_; }
    
    void addVariable(gc<String> name, gc<Object> value);
    int numVariables() const { return variables_.count(); }
    
    // Finds the previously-declared module-level variable with the given name.
    // Returns -1 if not found.
    int findVariable(gc<String> name);
    
    gc<Object> getVariable(int index) const { return variables_[index]; }
    gc<String> getVariableName(int index) const { return variableNames_[index]; }

    void setVariable(int index, gc<Object> value);
    
  private:
    // The code compromising a module is compiled to a fake method so that
    // loading a module is basically just executing a function call.
    gc<Chunk> body_;
    
    // The modules imported by this one.
    Array<Module*> imports_;
    
    // The top-level variables defined by this module.
    Array<gc<Object> > variables_;
    Array<gc<String> > variableNames_;
    
    NO_COPY(Module);
  };
}
