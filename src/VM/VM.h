#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"

namespace magpie
{
  class ClassObject;
  class Module;
  class ModuleAst;
  class RecordType;
  
  typedef gc<Object> (*Native)(ArrayView<gc<Object> >& args);

  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource
  {
  public:
    VM();

    virtual void reachRoots();
    
    bool loadProgram(const char* fileName, gc<String> source);
    void loadModule(Module* module);
    
    Module* createModule();
    
    Module* coreModule() { return coreModule_; }
    Module* getModule(int index) { return modules_[index]; }
    int getModuleIndex(Module& module) const;
    
    Fiber& fiber() { return *fiber_; }

    inline gc<Object> nothing() const { return nothing_; }
    
    inline gc<ClassObject> boolClass() const { return boolClass_; }
    inline gc<ClassObject> classClass() const { return classClass_; }
    inline gc<ClassObject> nothingClass() const { return nothingClass_; }
    inline gc<ClassObject> numberClass() const { return numberClass_; }
    inline gc<ClassObject> recordClass() const { return recordClass_; }
    inline gc<ClassObject> stringClass() const { return stringClass_; }
    inline gc<ClassObject> noMatchErrorClass() const { return noMatchErrorClass_; }
    
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
    
    int findNative(gc<String> name);
    Native getNative(int index) const { return natives_[index]; }
    
    int addRecordType(const Array<int>& fields);
    gc<RecordType> getRecordType(int id);
    
    symbolId addSymbol(gc<String> name);
    
    // Adds a method to the list of methods that have been compiled, but whose
    // definitions have not yet been executed.
    methodId addMethod(gc<Method> method);
    
    int declareMultimethod(gc<String> signature);
    int findMultimethod(gc<String> signature);
    void defineMethod(int multimethod, methodId method);
    gc<Chunk> getMultimethod(int multimethod);
    
  private:
    // Parses the given module source file. Returns null if there was a syntax
    // error.
    gc<ModuleAst> parseModule(const char* fileName, gc<String> source);
    
    void registerClass(gc<ClassObject>& classObj, const char* name);
    
    Array<Module*> modules_;
    Module* coreModule_;
    
    Array<gc<String> > nativeNames_;
    Array<Native> natives_;
    
    Array<gc<RecordType> > recordTypes_;
    // TODO(bob): Something more optimal than an O(n) array.
    Array<gc<String> > symbols_;
    
    Array<gc<Method> > methods_;
    Array<gc<Multimethod> > multimethods_;
    
    gc<Fiber> fiber_;
    
    gc<Object> true_;
    gc<Object> false_;
    gc<Object> nothing_;
    gc<ClassObject> boolClass_;
    gc<ClassObject> classClass_;
    gc<ClassObject> nothingClass_;
    gc<ClassObject> numberClass_;
    gc<ClassObject> recordClass_;
    gc<ClassObject> stringClass_;
    gc<ClassObject> noMatchErrorClass_;
    
    NO_COPY(VM);
  };
}

