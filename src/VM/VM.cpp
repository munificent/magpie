#include "VM.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Module.h"
#include "Object.h"
#include "Parser.h"
#include "Primitives.h"

#define DEF_PRIMITIVE(name, signature) \
        methods_.define(String::create(signature), name##Primitive); \

namespace magpie
{
  VM::VM()
  : modules_(),
    recordTypes_(),
    fiber_()
  {
    Memory::initialize(this, 1024 * 1024 * 2); // TODO(bob): Use non-magic number.
    
    fiber_ = new Fiber(*this);
    
    DEF_PRIMITIVE(print, "print 0:");
    DEF_PRIMITIVE(add, "0: + 0:");
    DEF_PRIMITIVE(subtract, "0: - 0:");
    DEF_PRIMITIVE(multiply, "0: * 0:");
    DEF_PRIMITIVE(divide, "0: / 0:");
    
    coreModule_ = createModule();
    
    makeClass(boolClass_, "Bool");
    makeClass(classClass_, "Class");
    makeClass(nothingClass_, "Nothing");
    makeClass(numberClass_, "Num");
    makeClass(recordClass_, "Record");
    makeClass(stringClass_, "String");
    
    true_ = new BoolObject(true);
    false_ = new BoolObject(false);
    nothing_ = new NothingObject();
  }

  bool VM::loadModule(const char* fileName, gc<String> source)
  {
    ErrorReporter reporter;
    Parser parser(fileName, source, reporter);
    gc<ModuleAst> moduleAst = parser.parseModule();
    
    if (reporter.numErrors() > 0) return false;
    
    // Compile it.
    Module* module = Compiler::compileModule(*this, moduleAst, reporter);

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
    methods_.reach();
    Memory::reach(recordTypes_);
    Memory::reach(fiber_);
    Memory::reach(true_);
    Memory::reach(false_);
    Memory::reach(nothing_);
    
    for (int i = 0; i < modules_.count(); i++)
    {
      modules_[i]->reach();
    }
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

  void VM::makeClass(gc<Object>& classObj, const char* name)
  {
    gc<String> nameString = String::create(name);
    classObj = new ClassObject(nameString);
    coreModule_->addExport(nameString, classObj);
  }
}

