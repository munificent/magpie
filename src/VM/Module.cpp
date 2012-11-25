#include "Compiler.h"
#include "Environment.h"
#include "Module.h"
#include "Memory.h"
#include "Method.h"
#include "Object.h"
#include "Parser.h"
#include "VM.h"

namespace magpie
{
  bool Module::parse()
  {
    ASSERT(ast_.isNull(), "Module is already parsed.");

    // TODO(bob): Better error handling.
    gc<String> source = readFile(path_);

    ErrorReporter reporter;
    Parser parser(path_, source, reporter);
    ast_ = parser.parseModule();

    if (reporter.numErrors() > 0) ast_ = NULL;
    return !ast_.isNull();
  }

  void Module::addImports(VM& vm)
  {
    // Implicitly import core (unless we are core).
    if (*name_ != "core")
    {
      vm.importModule(this, String::create("core"));
    }

    // Load all of the imports.
    for (int i = 0; i < ast_->body()->expressions().count(); i++)
    {
      ImportExpr* import = ast_->body()->expressions()[i]->asImportExpr();
      if (import == NULL) continue;

      // TODO(bob): Should the parser do this?
      gc<String> name = import->name()[0];
      for (int j = 1; j < import->name().count(); j++)
      {
        name = String::format("%s.%s", name->cString(), import->name()[j]->cString());
      }

      vm.importModule(this, name);
    }
  }

  bool Module::compile(VM& vm)
  {
    ASSERT(!ast_.isNull(), "Must parse module before compiling.");

    ErrorReporter reporter;
    Compiler::compileModule(vm, reporter, ast_, this);

    // Now that we've compiled it, we can throw away the AST.
    ast_ = NULL;

    return reporter.numErrors() == 0;
  }
  
  void Module::setBody(gc<Chunk> body)
  {
    body_ = body;
  }
  
  void Module::addVariable(gc<String> name, gc<Object> value)
  {
    variableNames_.add(name);
    variables_.add(value);
  }
  
  int Module::findVariable(gc<String> name)
  {
    for (int i = 0; i < variableNames_.count(); i++)
    {
      if (variableNames_[i] == name) return i;
    }
    
    return -1;
  }
  
  void Module::setVariable(int index, gc<Object> value)
  {
    variables_[index] = value;
  }

  void Module::reach()
  {
    name_.reach();
    path_.reach();
    ast_.reach();
    body_.reach();
    variables_.reach();
    variableNames_.reach();
  }
}

