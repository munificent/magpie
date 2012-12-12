#include "VM.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Environment.h"
#include "Module.h"
#include "Natives.h"
#include "Object.h"
#include "Parser.h"
#include "Path.h"

#define DEF_NATIVE(name) \
    nativeNames_.add(String::create(#name)); \
    natives_.add(name##Native);

namespace magpie
{
  // TODO(bob): Move somewhere else?
  struct ImportGraph
  {
    // Default constructor so we can use it in Array.
    ImportGraph()
    : module(NULL)
    {}

    ImportGraph(Module* module)
    : module(module),
    imports()
    {
      for (int i = 0; i < module->imports().count(); i++)
      {
        imports.add(module->imports()[i]);
      }
    }

    Module* module;
    Array<Module*> imports;
  };
  
  VM::VM()
  : modules_(),
    replModule_(NULL),
    nativeNames_(),
    natives_(),
    recordTypes_(),
    methods_(),
    multimethods_(),
    scheduler_(*this)
  {
    Memory::initialize(this, 1024 * 1024 * 2); // TODO(bob): Use non-magic number.
    
    DEF_NATIVE(bindCore);
    DEF_NATIVE(bindIO);
    DEF_NATIVE(objectClass);
    DEF_NATIVE(objectNew);
    DEF_NATIVE(objectToString);
    DEF_NATIVE(printString);
    DEF_NATIVE(numPlusNum);
    DEF_NATIVE(stringPlusString);
    DEF_NATIVE(numMinusNum);
    DEF_NATIVE(numTimesNum);
    DEF_NATIVE(numDivNum);
    DEF_NATIVE(numModNum);
    DEF_NATIVE(numLessThanNum);
    DEF_NATIVE(numLessThanEqualToNum);
    DEF_NATIVE(numGreaterThanNum);
    DEF_NATIVE(numGreaterThanEqualToNum);
    DEF_NATIVE(stringCount);
    DEF_NATIVE(numToString);
    DEF_NATIVE(channelClose);
    DEF_NATIVE(channelIsOpen);
    DEF_NATIVE(channelNew);
    DEF_NATIVE(channelReceive);
    DEF_NATIVE(channelSend);
    DEF_NATIVE(fileClose);
    DEF_NATIVE(fileIsOpen);
    DEF_NATIVE(fileOpen);
    DEF_NATIVE(fileRead);
    DEF_NATIVE(functionCall);
    DEF_NATIVE(listAdd);
    DEF_NATIVE(listCount);
    DEF_NATIVE(listIndex);
    DEF_NATIVE(listIndexSet);
    DEF_NATIVE(listInsert);

    true_ = new BoolObject(true);
    false_ = new BoolObject(false);
    nothing_ = new NothingObject();
  }

  void VM::bindCore()
  {
    Module* core = findModule("core");
    ASSERT_NOT_NULL(core);
    
    registerClass(core, boolClass_, "Bool");
    registerClass(core, channelClass_, "Channel");
    registerClass(core, classClass_, "Class");
    registerClass(core, functionClass_, "Function");
    registerClass(core, listClass_, "List");
    registerClass(core, nothingClass_, "Nothing");
    registerClass(core, numberClass_, "Num");
    registerClass(core, recordClass_, "Record");
    registerClass(core, stringClass_, "String");
    registerClass(core, noMatchErrorClass_, "NoMatchError");
    registerClass(core, noMethodErrorClass_, "NoMethodError");
    registerClass(core, undefinedVarErrorClass_, "UndefinedVarError");

    int index = core->findVariable(String::create("done"));
    done_ = core->getVariable(index);
  }

  void VM::bindIO()
  {
    Module* io = findModule("io");
    ASSERT_NOT_NULL(io);

    registerClass(io, fileClass_, "File");
  }

  bool VM::runProgram(gc<String> path)
  {
    // Remember where the program is so we can import modules from there.
    programDir_ = path::dir(path::real(path));

    Module* entrypoint = addModule(NULL, path);
    if (entrypoint == NULL) return false;

    // Sort the modules by their imports so that dependencies are run before
    // modules that depend on them.
    // See: http://en.wikipedia.org/wiki/Topological_sorting
    // Clone the import graph so we can destructively modify it.
    // TODO(bob): This does lots of array copies since ImportGraph stores an
    // array directly. Do something smarter here.
    Array<ImportGraph> graph;
    for (int i = 0; i < modules_.count(); i++)
    {
      graph.add(ImportGraph(modules_[i]));
    }

    Array<Module*> modules;
    while (graph.count() > 0)
    {
      bool madeProgress = false;
      for (int i = 0; i < graph.count(); i++)
      {
        // See if all of this module's imports are accounted for.
        if (graph[i].imports.count() == 0)
        {
          // They are, so it's ready to process.
          modules.add(graph[i].module);

          // And now everything that imports it doesn't have to worry about it
          // anymore.
          for (int j = 0; j < graph.count(); j++)
          {
            for (int k = 0; k < graph[j].imports.count(); k++)
            {
              if (graph[j].imports[k] == graph[i].module)
              {
                graph[j].imports.removeAt(k);
                k--;
              }
            }
          }

          graph.removeAt(i);
          i--;
          madeProgress = true;
        }
      }

      // Bail if there is an import cycle.
      // TODO(bob): Better error-handling.
      if (!madeProgress) return false;
    }
    
    // Run each loaded module.
    for (int i = 0; i < modules.count(); i++)
    {
      Module* module = modules[i];

      // Compile it.
      if (!module->compile(*this)) return false;

      scheduler_.runModule(module);
    }

    return true;
  }

  void VM::importModule(Module* from, gc<String> name)
  {
    Module* module = addModule(name, NULL);
    if (module == NULL) return;

    from->imports().add(module);
  }

  gc<Object> VM::evaluateReplExpression(gc<Expr> expr)
  {
    if (replModule_ == NULL)
    {
      replModule_ = new Module(String::create("<repl>"), String::create(""));
      modules_.add(replModule_);
      
      // Implicitly import core.
      importModule(replModule_, String::create("core"));
    }

    // Compile it.
    ErrorReporter reporter;
    Compiler::compileExpression(*this, reporter, expr, replModule_);
    if (reporter.numErrors() > 0) return gc<Object>();

    return scheduler_.runModule(replModule_);
  }

  int VM::getModuleIndex(Module& module) const
  {
    int index = modules_.indexOf(&module);
    ASSERT(index != -1, "Cannot get index of unknown module.");
    return index;
  }

  void VM::reachRoots()
  {
    programDir_.reach();
    recordTypes_.reach();
    scheduler_.reach();
    true_.reach();
    false_.reach();
    nothing_.reach();
    done_.reach();
    symbols_.reach();
    methods_.reach();
    
    for (int i = 0; i < modules_.count(); i++)
    {
      modules_[i]->reach();
    }
  }

  gc<Object> VM::getBuiltIn(BuiltIn value) const
  {
    switch (value) {
      case BUILT_IN_FALSE: return false_;
      case BUILT_IN_TRUE: return true_;
      case BUILT_IN_NOTHING: return nothing_;
      case BUILT_IN_NO_METHOD:
        return DynamicObject::create(noMethodErrorClass_);
      case BUILT_IN_DONE: return done_;
    }

    ASSERT(false, "Unknown built-in ID.");
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

  gc<Multimethod> VM::getMultimethod(int multimethodId)
  {
    return multimethods_[multimethodId];
  }

  Module* VM::addModule(gc<String> name, gc<String> path)
  {
    if (name.isNull())
    {
      ASSERT(!path.isNull(), "Must be given a path or a name.");

      // Infer the name from the script file name.
      // TODO(bob): Should take the file name without an extension.
      name = path;
    }
    
    // Make sure it hasn't already been added.
    // TODO(bob): Could make modules_ a hash table for performance.
    for (int i = 0; i < modules_.count(); i++)
    {
      if (modules_[i]->name() == name)
      {
        return modules_[i];
      }
    }

    // Locate the path to the module if we aren't given it.
    if (path.isNull())
    {
      ASSERT(!name.isNull(), "Must be given a path or a name.");
      path = locateModule(programDir_, name);

      // TODO(bob): Report error better.
      if (path.isNull()) return NULL;
    }

    Module* module = new Module(name, path);
    modules_.add(module);
    
    if (!module->parse()) return NULL;

    module->addImports(*this);

    return module;
  }

  void VM::registerClass(Module* module, gc<ClassObject>& classObj,
                         const char* name)
  {
    int index = module->findVariable(String::create(name));
    classObj = module->getVariable(index)->asClass();
  }

  Module* VM::findModule(const char* name)
  {
    for (int i = 0; i < modules_.count(); i++)
    {
      if (*modules_[i]->name() == name) return modules_[i];
    }

    ASSERT(false, "Could not find core module.");
    return NULL;
  }
}

