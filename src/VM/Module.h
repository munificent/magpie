#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"

namespace magpie
{
  class Chunk;
  class ModuleAst;
  class Object;

  // A module is a single file of compiled Magpie code.
  class Module
  {
  public:
    Module(gc<String> name, gc<String> path)
    : name_(name),
      path_(path),
      ast_(),
      body_(),
      imports_(),
      variables_(),
      variableNames_()
    {}

    // Gets the name of the module.
    gc<String> name() const { return name_; }

    bool parse();
    void addImports(VM& vm);
    bool compile(VM& vm);
    
    void setBody(gc<Chunk> body);
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

    void reach();

  private:
    // The name of the module. This is how it will be referenced in imports.
    gc<String> name_;
    
    // The path to the file the module was loaded from.
    gc<String> path_;

    // The parsed AST for the module. This will only be non-null after [load()]
    // has been called and before [compile()].
    gc<ModuleAst> ast_;

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
