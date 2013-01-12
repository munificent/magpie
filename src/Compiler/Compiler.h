#pragma once

#include "Array.h"
#include "Ast.h"
#include "Memory.h"

namespace magpie
{
  class Chunk;
  class ErrorReporter;
  class Method;
  class Module;
  class ModuleCompilation;
  class Multimethod;
  class VM;
  
  class Compiler
  {
  public:
    static void compileModule(VM& vm, ErrorReporter& reporter,
                              gc<ModuleAst> ast, Module* module);
    
    static gc<Chunk> compileMultimethod(VM& vm, ErrorReporter& reporter,
                                        Multimethod& multimethod);

    static void compileExpression(VM& vm, ErrorReporter& reporter,
                                  gc<Expr> expr, Module* module);

    ErrorReporter& reporter() { return reporter_; }
    
    int findMethod(gc<String> signature);
    
    methodId addMethod(gc<Method> method);
    symbolId addSymbol(gc<String> name);
    int addRecordType(Array<int>& nameSymbols);
    int getModuleIndex(Module& module);
    int findNative(gc<String> name);
    
  private:
    Compiler(VM& vm, ErrorReporter& reporter)
    : vm_(vm),
      reporter_(reporter)
    {}
    
    // Creates any names declared by top-level [expr]. Allows mututal recursion
    // of top level elements.
    void declareTopLevel(gc<Expr> expr, Module* module);
    
    void declareClass(DefClassExpr& classExpr, Module* module);
    gc<DefExpr> synthesizeConstructor(DefClassExpr& classExpr);
    gc<DefExpr> synthesizeGetter(DefClassExpr& classExpr, int fieldIndex);
    gc<DefExpr> synthesizeSetter(DefClassExpr& classExpr, int fieldIndex);

    int declareMultimethod(gc<String> signature);
    
    // Forward-declare any variables in the given pattern.
    void declareVariables(gc<Pattern> pattern, Module* module);
    
    void declareVariable(gc<SourcePos> pos, gc<String> name, Module* module);
    
    VM& vm_;
    ErrorReporter& reporter_;
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
    static gc<String> build(const CallExpr& expr, bool isLValue);
    
    // Builds a signature for the given method definition.
    static gc<String> build(const DefExpr& expr);
    
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
