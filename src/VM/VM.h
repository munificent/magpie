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
  class RecordType;
  
  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource
  {
  public:
    VM();

    virtual void reachRoots();
    
    bool loadModule(const char* fileName, gc<String> source);
    void loadModule(Module* module);
    
    Module* createModule();
    
    Module* coreModule() { return coreModule_; }
    Module* getModule(int index) { return modules_[index]; }
    int getModuleIndex(Module* module) const;
    
    Fiber& fiber() { return *fiber_; }

    // The globally available top-level methods.
    MethodScope& methods() { return methods_; }
    
    inline gc<Object> nothing() const { return nothing_; }
    
    inline gc<Object> boolClass() const { return boolClass_; }
    inline gc<Object> classClass() const { return classClass_; }
    inline gc<Object> nothingClass() const { return nothingClass_; }
    inline gc<Object> numberClass() const { return numberClass_; }
    inline gc<Object> recordClass() const { return recordClass_; }
    inline gc<Object> stringClass() const { return stringClass_; }
    
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
    
    int addRecordType(const Array<int>& fields);
    gc<RecordType> getRecordType(int id);
    
    symbolId addSymbol(gc<String> name);
    
  private:
    void makeClass(gc<Object>& classObj, const char* name);
    
    Array<Module*> modules_;
    Module* coreModule_;
    Array<gc<RecordType> > recordTypes_;
    // TODO(bob): Something more optimal than an O(n) array.
    Array<gc<String> > symbols_;
    MethodScope methods_;
    gc<Fiber> fiber_;
    
    gc<Object> true_;
    gc<Object> false_;
    gc<Object> nothing_;
    gc<Object> boolClass_;
    gc<Object> classClass_;
    gc<Object> nothingClass_;
    gc<Object> numberClass_;
    gc<Object> recordClass_;
    gc<Object> stringClass_;
    
    NO_COPY(VM);
  };
}

