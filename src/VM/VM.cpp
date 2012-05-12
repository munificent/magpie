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
    
    true_ = new BoolObject(true);
    false_ = new BoolObject(false);
    nothing_ = new NothingObject();

    coreModule_ = new Module();
    gc<String> boolName = String::create("Bool");
    coreModule_->addExport(boolName, new ClassObject(boolName));
    gc<String> stringName = String::create("String");
    coreModule_->addExport(stringName, new ClassObject(stringName));
  }

  bool VM::loadModule(const char* fileName, gc<String> source)
  {
    ErrorReporter reporter;
    Parser parser(fileName, source, reporter);
    gc<Node> moduleAst = parser.parseModule();
    
    if (reporter.numErrors() > 0) return false;
    
    // Compile it.
    Module* module = Compiler::compileModule(*this, moduleAst, reporter);
    
    if (reporter.numErrors() > 0) return false;
    
    loadModule(module);
    return true;
  }

  void VM::loadModule(Module* module)
  {
    modules_.add(module);
    fiber_->init(module->body());
    
    while (true)
    {
      gc<Object> result = fiber_->run();
      
      // If the fiber returns null, it's still running but it did a GC run.
      // Since that moves the fiber, we return back to here so we can invoke
      // run() again at its new location in memory.
      if (!result.isNull()) return;
    }
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
}

