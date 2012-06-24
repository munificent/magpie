#pragma once

#include "Array.h"
#include "Ast.h"
#include "Memory.h"

namespace magpie
{
  class ErrorReporter;
  class Method;
  class Module;
  class ModuleCompilation;
  class Multimethod;
  class VM;
  
  class Compiler : private DefVisitor
  {
  public:
    static Module* compileProgram(VM& vm, gc<ModuleAst> coreAst,
                                  gc<ModuleAst> module,
                                  ErrorReporter& reporter);

    ErrorReporter& reporter() { return reporter_; }
    
    void addMethod(gc<String> signature, gc<MethodDef> method, Module* module);
    int findMethod(gc<String> signature);
    
    int addSymbol(gc<String> name);
    int addRecordType(Array<int>& nameSymbols);
    int getModuleIndex(Module* module);
    int findNative(gc<String> name);
    
  private:
    static gc<Method> compileMethod(Compiler& compiler, Module* module,
                                    MethodDef& method,
                                    ErrorReporter& reporter);
    
    Compiler(VM& vm, ErrorReporter& reporter)
    : vm_(vm),
      reporter_(reporter),
      multimethods_()
    {}
    
    void visit(MethodDef& def, Module* module);
    
    void declareModule(gc<ModuleAst> moduleAst, Module* module);
    void compileMultimethods();
    
    VM& vm_;
    ErrorReporter& reporter_;
    
    Array<gc<ModuleCompilation> > modules_;
    // TODO(bob): Make this a map.
    Array<gc<Multimethod> > multimethods_;
  };
  
  // A module being compiled.
  // TODO(bob): Should this be gc, or just a value type?
  class ModuleCompilation : public Managed
  {
  public:
    ModuleCompilation(gc<ModuleAst> ast, Module* module)
    : ast_(ast),
      module_(module)
    {}
    
    gc<ModuleAst> ast() { return ast_; }
    Module* module() { return module_; }
    
    virtual void reach();

  private:
    gc<ModuleAst> ast_;
    Module* module_;
  };
  
  // A single method definition in a multimethod, and the context where it was
  // defined so that it can be compiled correctly.
  // TODO(bob): Should this be gc, or just a value type?
  class MethodInstance : public Managed
  {
  public:
    MethodInstance(gc<MethodDef> def, Module* module)
    : def_(def),
      module_(module)
    {}
    
    gc<MethodDef> def() { return def_; }
    Module* module() { return module_; }
    
    virtual void reach();

  private:
    gc<MethodDef> def_;
    Module* module_;
  };
  
  // Collects all of the method definitions for a given signature so that they
  // can be compiled to a single bytecode method all at once.
  class Multimethod : public Managed
  {
  public:
    Multimethod(gc<String> signature)
    : signature_(signature)
    {}
    
    gc<String> signature() { return signature_; }
    void addMethod(gc<MethodDef> method, Module* module);
    Array<gc<MethodInstance> > methods() { return methods_; }
    
    virtual void reach();

  private:
    gc<String> signature_;
    Array<gc<MethodInstance> > methods_;
  };

  // Method definitions and calls are statically distinguished by the records
  // used for the left and right arguments (or parameters). For example, a
  // call to "foo(1, b: 2)" is statically known to be different from a call to
  // "foo(1)" or "1 foo".
  //
  // This class supports that by generating a "signature" string for a method
  // definition or call that contains both the method's name, and the structure
  // of its arguments. The above examples would have signatures "foo(,b)",
  // "foo()", and "()foo" respectively.
  class SignatureBuilder
  {
  public:
    // Builds a signature for the method being called by the given expr.
    static gc<String> build(const CallExpr& expr);
    
    // Builds a signature for the given method definition.
    static gc<String> build(const MethodDef& expr);
    
  private:
    SignatureBuilder()
    : length_(0)
    {}
    
    static const int MAX_LENGTH = 256; // TODO(bob): Use something dynamic.
    
    void writeArg(gc<Expr> expr);
    void writeParam(gc<Pattern> pattern);
    void add(gc<String> text);
    void add(const char* text);
    
    int length_;
    char signature_[MAX_LENGTH];
  };
}