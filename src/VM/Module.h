#pragma once

#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Method;
  class Object;
  
  // A module is a single file of compiled Magpie code.
  class Module
  {
  public:
    Module()
    : body_(),
      imports_()
    {}
    
    void reach();
    
    void bindBody(gc<Method> body);
    gc<Method> body() const { return body_; }
    
    Array<Module*>& imports() { return imports_; }
    const Array<Module*>& imports() const { return imports_; }
    
    void addExport(gc<String> name, gc<Object> value);
    int numExports() const { return exports_.count(); }
    gc<Object> getExport(int index) const { return exports_[index]; }
    gc<String> getExportName(int index) const { return exportNames_[index]; }
    
  private:
    // The code compromising a module is compiled to a fake method so that
    // loading a module is basically just executing a function call.
    gc<Method> body_;
    
    // The modules imported by this one.
    Array<Module*> imports_;
    
    // The top-level variables exported by this module.
    Array<gc<Object> > exports_;
    Array<gc<String> > exportNames_;
    
    NO_COPY(Module);
  };
}
