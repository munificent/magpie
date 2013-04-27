#pragma once

#include "Common.h"
#include "Memory/Memory.h"
#include "Memory/RootSource.h"
#include "Syntax/Lexer.h"
#include "VM/Fiber.h"
#include "VM/Method.h"
#include "VM/Scheduler.h"

#define DEF_NATIVE(name) vm.defineNative(#name, name##Native);

namespace magpie
{
  class ClassObject;
  class ErrorReporter;
  class FunctionObject;
  class Expr;
  class Module;
  class ModuleAst;
  class RecordType;

  typedef gc<Object> (*Native)(VM& vm, Fiber& fiber,
                               ArrayView<gc<Object> >& args,
                               NativeResult& result);

  // Identifies classes that are defined in core lib but have a native C++
  // object class. The VM stores a reference to each class object so that it
  // can get the class for a given C++ object.
  enum CoreClass
  {
    // core
    CLASS_BOOL = 0,
    CLASS_CHANNEL,
    CLASS_CHAR,
    CLASS_CLASS,
    CLASS_DONE,
    CLASS_FLOAT,
    CLASS_FUNCTION,
    CLASS_INT,
    CLASS_LIST,
    CLASS_NOTHING,
    CLASS_RECORD,
    CLASS_STRING,
    CLASS_NO_MATCH_ERROR,
    CLASS_NO_METHOD_ERROR,
    CLASS_UNDEFINED_VAR_ERROR,

    // io
    CLASS_BUFFER,
    CLASS_FILE,
    CLASS_STREAM,

    // new
    CLASS_TCP_LISTENER,

    CLASS_MAX
  };

  // The main Virtual Machine class for a running Magpie interpreter.
  class VM : public RootSource
  {
  public:
    VM();

    virtual void reachRoots();

    bool runProgram(gc<String> path);

    // Gets the directory containing the main program file being executed.
    gc<String> programDir() const { return programDir_; }

    void importModule(ErrorReporter& reporter, Module* from, gc<SourcePos> pos,
                      gc<String> name);

    bool initRepl();
    gc<Object> evaluateReplExpression(gc<Expr> expr);

    Module* getModule(int index) { return modules_[index]; }
    int getModuleIndex(Module& module) const;

    inline gc<Object> nothing() const { return nothing_; }

    inline gc<Object> getBool(bool value) const
    {
      return value ? true_ : false_;
    }

    gc<Object> getAtom(Atom atom);

    void defineNative(const char* name, Native native);
    int findNative(gc<String> name);
    Native getNative(int index) const { return natives_[index]; }

    int addRecordType(const Array<int>& fields);
    gc<RecordType> getRecordType(int id);

    symbolId addSymbol(gc<String> name);

    // Gets the text for the symbol with the given ID.
    gc<String> getSymbol(symbolId symbol) const;

    // Adds a method to the list of methods that have been compiled, but whose
    // definitions have not yet been executed.
    methodId addMethod(gc<Method> method);

    int declareMultimethod(gc<String> signature);
    int findMultimethod(gc<String> signature);
    void defineMethod(int multimethod, methodId method);
    gc<Multimethod> getMultimethod(int multimethod);

    // Looks up a top-level class variable named [name] inside [module] and
    // binds that value as [coreClass].
    void bindClass(const char* module, CoreClass core, const char* name);

    gc<ClassObject> getClass(CoreClass core);

    // Sends [error], which is an error that [fiber] did not catch, to the
    // error-displaying fiber so it can be converted to a string and shown to
    // the user.
    void printUncaughtError(gc<Fiber> fiber, gc<Object> error);

  private:
    // Loads module [name] from [path] and the recursively loads its imports.
    // [from] is the module that's depending on the added one. If [path] is
    // NULL, then it will try to determine it from the name by searching the
    // file system. If [name] is NULL, it will infer it from the path.
    Module* addModule(ErrorReporter& reporter, gc<String> name,
                      gc<String> path);

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
    gc<ChannelObject> errorChannel_;

    // References to class objects for classes that are defined in the core lib
    // but have native C++ implementations.
    gc<ClassObject> coreClasses_[CLASS_MAX];

    NO_COPY(VM);
  };
}

