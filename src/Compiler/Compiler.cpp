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
  Module* Compiler::compileModule(VM& vm, ErrorReporter& reporter,
                                  gc<ModuleAst> ast, bool importCore)
  {
    Compiler compiler(vm, reporter);
    
    Module* module = vm.createModule();

    if (importCore) module->imports().add(vm.coreModule());
    
    compiler.declareTopLevel(ast, module);
    
    gc<Chunk> code = MethodCompiler(compiler).compileBody(module, ast->body());
    module->bindBody(code);
    return module;
  }
  
  gc<Chunk> Compiler::compileMultimethod(VM& vm, ErrorReporter& reporter,
                                         Multimethod& multimethod)
  {
    Compiler compiler(vm, reporter);
    return MethodCompiler(compiler).compile(multimethod);
  }
  
  int Compiler::findMethod(gc<String> signature)
  {
    return vm_.findMultimethod(signature);
  }
  
  int Compiler::declareMultimethod(gc<String> signature)
  {
    return vm_.declareMultimethod(signature);
  }

  methodId Compiler::addMethod(gc<Method> method)
  {
    return vm_.addMethod(method);
  }
  
  symbolId Compiler::addSymbol(gc<String> name)
  {
    return vm_.addSymbol(name);
  }

  int Compiler::addRecordType(Array<int>& nameSymbols)
  {
    return vm_.addRecordType(nameSymbols);
  }

  int Compiler::getModuleIndex(Module& module)
  {
    return vm_.getModuleIndex(module);
  }
    
  int Compiler::findNative(gc<String> name)
  {
    return vm_.findNative(name);
  }

  void Compiler::declareTopLevel(gc<ModuleAst> moduleAst, Module* module)
  {
    for (int i = 0; i < moduleAst->body()->expressions().count(); i++)
    {
      gc<Expr> expr = moduleAst->body()->expressions()[i];
      
      DefExpr* def = expr->asDefExpr();
      if (def != NULL)
      {
        declareMultimethod(SignatureBuilder::build(*def));
      }
      
      VariableExpr* var = expr->asVariableExpr();
      if (var != NULL)
      {
        declareVariables(var->pattern(), module);
      }
      
      // TODO(bob): Handle 'defclass' here too.
    }
  }
  
  void Compiler::declareVariables(gc<Pattern> pattern, Module* module)
  {
    RecordPattern* record = pattern->asRecordPattern();
    if (record != NULL)
    {
      for (int i = 0; i < record->fields().count(); i++)
      {
        declareVariables(record->fields()[i].value, module);
      }
      
      return;
    }
    
    VariablePattern* variable = pattern->asVariablePattern();
    if (variable != NULL)
    {
      // Make sure there isn't already a top-level variable with that name.
      int existing = module->findVariable(variable->name());
      if (existing != -1)
      {
        reporter_.error(pattern->pos(),
            "There is already a variable '%s' defined in this module.",
            variable->name()->cString());
      }
      
      module->addVariable(variable->name(), gc<Object>());
      
      if (!variable->pattern().isNull())
      {
        declareVariables(variable->pattern(), module);
      }
    }
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
  
  gc<String> SignatureBuilder::build(const DefExpr& method)
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