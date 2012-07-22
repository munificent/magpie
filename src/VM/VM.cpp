#include "VM.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Module.h"
#include "Natives.h"
#include "Object.h"
#include "Parser.h"

#define DEF_NATIVE(name, desc) \
    nativeNames_.add(String::create(desc)); \
    natives_.add(name##Native);

namespace magpie
{
  VM::VM()
  : modules_(),
    nativeNames_(),
    natives_(),
    recordTypes_(),
    methods_(),
    multimethods_(),
    fiber_()
  {
    Memory::initialize(this, 1024 * 1024 * 2); // TODO(bob): Use non-magic number.
    
    fiber_ = new Fiber(*this);
        
    DEF_NATIVE(print, "print");
    DEF_NATIVE(add, "num +");
    DEF_NATIVE(subtract, "num -");
    DEF_NATIVE(multiply, "num *");
    DEF_NATIVE(divide, "num /");
        
    true_ = new BoolObject(true);
    false_ = new BoolObject(false);
    nothing_ = new NothingObject();
  }

  bool VM::loadProgram(const char* fileName, gc<String> source)
  {
    ErrorReporter reporter;
    
    // Load the core module.
    // TODO(bob): Put this in an actual file somewhere.
    /*
    const char* coreSource =
        "def (is Num) + (is Num) native \"num +\"\n"
        "def (is Num) - (is Num) native \"num -\"\n"
        "def (is Num) * (is Num) native \"num *\"\n"
        "def (is Num) / (is Num) native \"num /\"\n"
        "def print(arg) native \"print\"\n";
     */
    // TODO(bob): Type annotate args when core module can figure out what "Num"
    // is bound to.
    const char* coreSource =
        "def (_) + (_) native \"num +\"\n"
        "def (_) - (_) native \"num -\"\n"
        "def (_) * (_) native \"num *\"\n"
        "def (_) / (_) native \"num /\"\n"
        "def print(arg) native \"print\"\n";
    gc<ModuleAst> coreAst = parseModule("<core>", String::create(coreSource));
    coreModule_ = Compiler::compileModule(*this, reporter, coreAst, false); 
    loadModule(coreModule_);
    
    makeClass(boolClass_, "Bool");
    makeClass(classClass_, "Class");
    makeClass(nothingClass_, "Nothing");
    makeClass(numberClass_, "Num");
    makeClass(recordClass_, "Record");
    makeClass(stringClass_, "String");
    
    gc<ModuleAst> moduleAst = parseModule(fileName, source);
    if (moduleAst.isNull()) return false;
    
    // Compile it.
    Module* module = Compiler::compileModule(*this, reporter, moduleAst, true);

    if (reporter.numErrors() > 0) return false;
    
    loadModule(module);
    return true;
  }

  void VM::loadModule(Module* module)
  {
    fiber_->init(module->body());
    
    FiberResult result;
    while ((result = fiber_->run()) == FIBER_DID_GC)
    {
      // If the fiber returns FIBER_DID_GC, it's still running but it did a GC.
      // Since that moves the fiber, we return back to here so we can invoke
      // run() again at its new location in memory.
    }
    
    // TODO(bob): Kind of hackish.
    // If we got an uncaught error while loading the module, exit with an error.
    if (result == FIBER_UNCAUGHT_ERROR)
    {
      exit(3);
    }
  }

  Module* VM::createModule()
  {
    Module* module = new Module();
    modules_.add(module);
    return module;
  }
  
  int VM::getModuleIndex(Module* module) const
  {
    int index = modules_.indexOf(module);
    ASSERT(index != -1, "Cannot get index of unknown module.");
    return index;
  }

  void VM::reachRoots()
  {
    Memory::reach(recordTypes_);
    Memory::reach(fiber_);
    Memory::reach(true_);
    Memory::reach(false_);
    Memory::reach(nothing_);
    Memory::reach(symbols_);
    Memory::reach(methods_);
    
    for (int i = 0; i < modules_.count(); i++)
    {
      modules_[i]->reach();
    }
  }
  
  int VM::findNative(gc<String> name)
  {
    for (int i = 0; i < nativeNames_.count(); i++)
    {
      if (nativeNames_[i] == name) return i;
    }

    return -1;
  }

  int VM::addRecordType(const Array<int>& fields)
  {
    // TODO(bob): Should use a hash table or something more optimal.
    // See if we already have a type for this signature.
    for (int i = 0; i < recordTypes_.count(); i++)
    {
      RecordType& type = *recordTypes_[i];
      
      // See if this record type matches what we're looking for.
      if (fields.count() != type.numFields()) continue;
      
      bool found = true;
      for (int j = 0; j < fields.count(); j++)
      {
        // Compare the symbol IDs of the records.
        // TODO(bob): Note, assumes symbols are sorted in both records.
        if (type.getSymbol(j) != fields[j]) {
          found = false;
          break;
        }
      }
      
      if (found) return i;
    }
    
    // It's a new type, so add it.
    gc<RecordType> type = RecordType::create(fields);
    recordTypes_.add(type);
    return recordTypes_.count() - 1;
  }

  gc<RecordType> VM::getRecordType(int id)
  {
    return recordTypes_[id];
  }
  
  symbolId VM::addSymbol(gc<String> name)
  {
    // See if it's already in the table.
    for (int i = 0; i < symbols_.count(); i++)
    {
      if (*name == *symbols_[i]) return i;
    }
    
    // It's a new symbol.
    symbols_.add(name);
    return symbols_.count() - 1;
  }

  methodId VM::addMethod(gc<Method> method)
  {
    methods_.add(method);
    return methods_.count() - 1;
  }
  
  int VM::declareMultimethod(gc<String> signature)
  {
    // See if it's already declared.
    int index = findMultimethod(signature);
    if (index != -1) return index;
    
    // It's a new multimethod.
    multimethods_.add(new Multimethod(signature));
    return multimethods_.count() - 1;
  }
  
  int VM::findMultimethod(gc<String> signature)
  {
    for (int i = 0; i < multimethods_.count(); i++)
    {
      if (signature == multimethods_[i]->signature()) return i;
    }
    
    // Not found.
    return -1;
  }

  void VM::defineMethod(int multimethod, methodId method)
  {
    multimethods_[multimethod]->addMethod(methods_[method]);
  }

  gc<Chunk> VM::getMultimethod(int multimethodId)
  {
    gc<Multimethod> multimethod = multimethods_[multimethodId];
    return multimethod->getChunk(*this);
  }

  gc<ModuleAst> VM::parseModule(const char* fileName, gc<String> source)
  {
    ErrorReporter reporter;
    Parser parser(fileName, source, reporter);
    gc<ModuleAst> moduleAst = parser.parseModule();
    
    if (reporter.numErrors() > 0) return gc<ModuleAst>();
    
    return moduleAst;
  }

  void VM::makeClass(gc<Object>& classObj, const char* name)
  {
    gc<String> nameString = String::create(name);
    classObj = new ClassObject(nameString);
    coreModule_->addExport(nameString, classObj);
  }
}

