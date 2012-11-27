#pragma once

#include "Fiber.h"
#include "Lexer.h"
#include "Macros.h"
#include "Memory.h"
#include "Method.h"
#include "RootSource.h"
#include "Scheduler.h"

namespace magpie
{
  class ClassObject;
  class FunctionObject;
  class Expr;
  class Module;
  class ModuleAst;
  class RecordType;
  
  typedef gc<Object> (*Native)(VM& vm, Fiber& fiber,
                               ArrayView<gc<Object> >& args,
                               NativeResult& result);

  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource
  {
  public:
    VM();

    virtual void reachRoots();
    
    // This is called by a native method at the end of the core library so the
    // VM can register the types defined there that it cares about.
    void bindCore();

    // This is called by a native method at the end of the io library so the
    // VM can register the types defined there that it cares about.
    void bindIO();

    bool runProgram(gc<String> path);

    // Gets the directory containing the main program file being executed.
    gc<String> programDir() const { return programDir_; }

    void importModule(Module* from, gc<String> name);

    gc<Object> evaluateReplExpression(gc<Expr> expr);

    Module* getModule(int index) { return modules_[index]; }
    int getModuleIndex(Module& module) const;

    inline gc<Object> nothing() const { return nothing_; }
    
    inline gc<ClassObject> boolClass() const { return boolClass_; }
    inline gc<ClassObject> channelClass() const { return channelClass_; }
    inline gc<ClassObject> classClass() const { return classClass_; }
    inline gc<ClassObject> fileClass() const { return fileClass_; }
    inline gc<ClassObject> functionClass() const { return functionClass_; }
    inline gc<ClassObject> listClass() const { return listClass_; }
    inline gc<ClassObject> nothingClass() const { return nothingClass_; }
    inline gc<ClassObject> numberClass() const { return numberClass_; }
    inline gc<ClassObject> recordClass() const { return recordClass_; }
    inline gc<ClassObject> stringClass() const { return stringClass_; }
    inline gc<ClassObject> noMatchErrorClass() const { return noMatchErrorClass_; }
    inline gc<ClassObject> noMethodErrorClass() const { return noMethodErrorClass_; }
    inline gc<ClassObject> undefinedVarErrorClass() const { return undefinedVarErrorClass_; }

    inline gc<Object> getBool(bool value) const
    {
      return value ? true_ : false_;
    }
    
    gc<Object> getBuiltIn(BuiltIn value) const;

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
    gc<Multimethod> getMultimethod(int multimethod);

    // Adds a new fiber to the scheduler.
    void addFiber(gc<Fiber> fiber);

  private:
    // Loads module [name] from [path] and the recursively loads its imports.
    // [from] is the module that's depending on the added one. If [path] is
    // NULL, then it will try to determine it from the name by searching the
    // file system. If [name] is NULL, it will infer it from the path.
    Module* addModule(gc<String> name, gc<String> path);

    gc<Object> runModule(Module* module);

    void registerClass(Module* module, gc<ClassObject>& classObj,
                       const char* name);

    Module* findModule(const char* name);

    gc<String> programDir_;

    Array<Module*> modules_;
    Module* replModule_;
    
    Array<gc<String> > nativeNames_;
    Array<Native> natives_;
    
    Array<gc<RecordType> > recordTypes_;
    // TODO(bob): Something more optimal than an O(n) array.
    Array<gc<String> > symbols_;
    
    Array<gc<Method> > methods_;
    Array<gc<Multimethod> > multimethods_;

    Scheduler scheduler_;

    gc<Object> true_;
    gc<Object> false_;
    gc<Object> nothing_;
    gc<Object> done_;
    gc<ClassObject> boolClass_;
    gc<ClassObject> channelClass_;
    gc<ClassObject> classClass_;
    gc<ClassObject> fileClass_;
    gc<ClassObject> functionClass_;
    gc<ClassObject> listClass_;
    gc<ClassObject> nothingClass_;
    gc<ClassObject> numberClass_;
    gc<ClassObject> recordClass_;
    gc<ClassObject> stringClass_;
    gc<ClassObject> noMatchErrorClass_;
    gc<ClassObject> noMethodErrorClass_;
    gc<ClassObject> undefinedVarErrorClass_;

    NO_COPY(VM);
  };
}

