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
    coreModule_(NULL),
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
    DEF_NATIVE(addNum, "num +");
    DEF_NATIVE(addString, "string +");
    DEF_NATIVE(subtract, "num -");
    DEF_NATIVE(multiply, "num *");
    DEF_NATIVE(divide, "num /");
    DEF_NATIVE(stringCount, "string count");
        
    true_ = new BoolObject(true);
    false_ = new BoolObject(false);
    nothing_ = new NothingObject();
  }

  void VM::init()
  {
    // Load the core module.
    // TODO(bob): Put this in an actual file somewhere.
    gc<String> coreSource = String::create(
        "defclass Bool\n"
        "end\n"
        "defclass Class\n"
        "end\n"
        "defclass Nothing\n"
        "end\n"
        "defclass Num\n"
        "end\n"
        "defclass Record\n"
        "end\n"
        "defclass String\n"
        "end\n"
        "defclass NoMatchError\n"
        "end\n"
        "def (is Num) + (is Num) native \"num +\"\n"
        "def (is String) + (is String) native \"string +\"\n"
        "def (is Num) - (is Num) native \"num -\"\n"
        "def (is Num) * (is Num) native \"num *\"\n"
        "def (is Num) / (is Num) native \"num /\"\n"
        "def print(arg) native \"print\"\n"
        "def (is String) count native \"string count\"\n");
    coreModule_ = compileModule("<core>", coreSource);
    runModule(coreModule_);
    
    registerClass(boolClass_, "Bool");
    registerClass(classClass_, "Class");
    registerClass(nothingClass_, "Nothing");
    registerClass(numberClass_, "Num");
    registerClass(recordClass_, "Record");
    registerClass(stringClass_, "String");
    registerClass(noMatchErrorClass_, "NoMatchError");
  }

  bool VM::loadModule(const char* fileName, gc<String> source)
  {
    Module* module = compileModule(fileName, source);
    if (module == NULL) return false;
    
    runModule(module);
    return true;
  }
  
  int VM::getModuleIndex(Module& module) const
  {
    int index = modules_.indexOf(&module);
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

  void VM::registerClass(gc<ClassObject>& classObj, const char* name)
  {
    int index = coreModule_->findVariable(String::create(name));
    classObj = coreModule_->getVariable(index)->toClass();
  }
  
  Module* VM::compileModule(const char* fileName, gc<String> source)
  {
    // Parse it.
    gc<ModuleAst> ast = parseModule(fileName, source);
    if (ast.isNull()) return NULL;
    
    Module* module = new Module();
    modules_.add(module);
    
    // Compile it.
    ErrorReporter reporter;
    Compiler::compileModule(*this, reporter, ast, module);
    if (reporter.numErrors() > 0) return NULL;
    
    return module;
  }
  
  void VM::runModule(Module* module)
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
}

