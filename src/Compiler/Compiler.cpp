#include "Ast.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "MethodCompiler.h"
#include "Module.h"
#include "Object.h"
#include "Resolver.h"
#include "VM.h"

namespace magpie
{
  Module* Compiler::compileProgram(VM& vm, gc<ModuleAst> coreAst,
                                   gc<ModuleAst> moduleAst,
                                   ErrorReporter& reporter)
  {
    Compiler compiler(vm, reporter);
    
    Module* module = vm.createModule();
    
    // TODO(bob): Doing this here is hackish. Need to figure out when a module's
    // imports are resolved.
    module->imports().add(vm.coreModule());
    
    // TODO(bob): Need to do this on all modules in the import graph.
    compiler.declareModule(coreAst, vm.coreModule());
    compiler.declareModule(moduleAst, module);
    compiler.compileMultimethods();
    
    // Wrap the module's imperative code in a shell method and compile it.
    gc<Expr> body = moduleAst->body();
    MethodDef* method = new MethodDef(body->pos(), gc<Pattern>(),
        String::create("<module>"), gc<Pattern>(), body);
    
    module->bindBody(compileMethod(compiler, module, *method, reporter));
    return module;
  }
  
  void Compiler::addMethod(gc<String> signature, gc<MethodDef> method,
                           Module* module)
  {
    // See if we already have a multimethod with that signature.
    int index;
    for (index = 0; index < multimethods_.count(); index++)
    {
      if (multimethods_[index]->signature() == signature) break;
    }
    
    if (index == multimethods_.count())
    {
      // A new multimethod, so add it.
      multimethods_.add(new Multimethod(signature));
    }
    
    multimethods_[index]->addMethod(method, module);
  }
  
  int Compiler::findMethod(gc<String> signature)
  {
    for (int i = 0; i < multimethods_.count(); i++)
    {
      if (multimethods_[i]->signature() == signature) return i;
    }
    
    return -1;
  }

  int Compiler::addSymbol(gc<String> name)
  {
    return vm_.addSymbol(name);
  }

  int Compiler::addRecordType(Array<int>& nameSymbols)
  {
    return vm_.addRecordType(nameSymbols);
  }

  int Compiler::getModuleIndex(Module* module)
  {
    return vm_.getModuleIndex(module);
  }
    
  int Compiler::findNative(gc<String> name)
  {
    return vm_.findNative(name);
  }

  gc<Chunk> Compiler::compileMethod(Compiler& compiler, Module* module,
                                     MethodDef& method,
                                     ErrorReporter& reporter)
  {
    return MethodCompiler(compiler, module).compile(method);
  }
  
  void Compiler::visit(MethodDef& def, Module* module)
  {
    gc<String> signature = SignatureBuilder::build(def);
    addMethod(signature, &def, module);
  }
  
  void Compiler::declareModule(gc<ModuleAst> moduleAst, Module* module)
  {
    modules_.add(new ModuleCompilation(moduleAst, module));
    
    // Group all of the methods together into multimethods. This also
    // effectively forward-declares them, so we know their index.
    for (int i = 0; i < moduleAst->defs().count(); i++)
    {
      moduleAst->defs()[i]->accept(*this, module);
    }
  }
  
  void Compiler::compileMultimethods()
  {
    // Compile all of the multimethods.
    for (int i = 0; i < multimethods_.count(); i++)
    {
      gc<Multimethod> multimethod = multimethods_[i];
      
      ASSERT(multimethod->methods().count() == 1,
             "Multimethods aren't implemented yet.");
      
      gc<MethodInstance> method = multimethod->methods()[0];
      gc<Chunk> compiled = MethodCompiler(*this, method->module()).compile(*method->def());
      
      vm_.methods().define(multimethod->signature(), compiled);
    }
  }
  
  void ModuleCompilation::reach()
  {
    Memory::reach(ast_);
  }

  void MethodInstance::reach()
  {
    Memory::reach(def_);
  }
  
  void Multimethod::addMethod(gc<MethodDef> method, Module* module)
  {
    methods_.add(new MethodInstance(method, module));
  }
  
  void Multimethod::reach()
  {
    Memory::reach(signature_);
    Memory::reach(methods_);
  }
  
  gc<String> SignatureBuilder::build(const CallExpr& expr)
  {
    // 1 foo                 -> ()foo
    // 1 foo()               -> ()foo
    // 1 foo(2)              -> ()foo()
    // foo(1)                -> foo()
    // (1, 2) foo            -> (,)foo
    // foo(1, b: 2, 3, e: 4) -> foo(,b,,e)
    SignatureBuilder builder;
    
    if (!expr.leftArg().isNull())
    {
      builder.writeArg(expr.leftArg());
      builder.add(" ");
    }
    
    builder.add(expr.name()->cString());

    if (!expr.rightArg().isNull())
    {
      builder.add(" ");
      builder.writeArg(expr.rightArg());
    }
    
    return String::create(builder.signature_, builder.length_);
  }
  
  gc<String> SignatureBuilder::build(const MethodDef& method)
  {
    // def (a) foo               -> ()foo
    // def (a) foo()             -> ()foo
    // def (a) foo(b)            -> ()foo()
    // def foo(b)                -> foo()
    // def (a, b) foo            -> (,)foo
    // def foo(a, b: c, d, e: f) -> foo(,b,,e)
    SignatureBuilder builder;
    
    if (!method.leftParam().isNull())
    {
      builder.writeParam(method.leftParam());
      builder.add(" ");
    }
    
    builder.add(method.name()->cString());
    
    if (!method.rightParam().isNull())
    {
      builder.add(" ");
      builder.writeParam(method.rightParam());
    }
    
    return String::create(builder.signature_, builder.length_);
  }
  
  void SignatureBuilder::writeArg(gc<Expr> expr)
  {
    // TODO(bob): Clean up. Redundant with build().
    // If it's a record, destructure it into the signature.
    RecordExpr* record = expr->asRecordExpr();
    if (record != NULL)
    {
      for (int i = 0; i < record->fields().count(); i++)
      {
        add(record->fields()[i].name);
        add(":");
      }
      
      return;
    }
    
    // Right now, all other exprs mean "some arg goes here".
    add("0:");
  }

  void SignatureBuilder::writeParam(gc<Pattern> pattern)
  {
    // If it's a record, destructure it into the signature.
    RecordPattern* record = pattern->asRecordPattern();
    if (record != NULL)
    {
      for (int i = 0; i < record->fields().count(); i++)
      {
        add(record->fields()[i].name);
        add(":");
      }
      
      return;
    }
    
    // Any other pattern is implicitly a single-field record.
    add("0:");
  }
  
  void SignatureBuilder::add(gc<String> text)
  {
    add(text->cString());
  }
  
  void SignatureBuilder::add(const char* text)
  {
    int length = strlen(text);
    ASSERT(length_ + length < MAX_LENGTH, "Signature too long.");
    
    strcpy(signature_ + length_, text);
    length_ += strlen(text);
  }
}