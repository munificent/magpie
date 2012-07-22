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
    
    compiler.declareModule(ast, module);
    
    // TODO(bob): Move this new Seq into parser.
    gc<Expr> body = new SequenceExpr(SourcePos(NULL, 0, 0, 0, 0), ast->exprs());
    gc<Chunk> code = MethodCompiler(compiler, module).compileBody(body);
    module->bindBody(code);
    return module;
  }
  
  gc<Chunk> Compiler::compileMultimethod(VM& vm, ErrorReporter& reporter,
                                         Multimethod& multimethod)
  {
    Compiler compiler(vm, reporter);
    gc<Method> method = multimethod.hackGetMethod();
    return MethodCompiler(compiler, method->module()).compileTemp(*method->def());
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

  int Compiler::getModuleIndex(Module* module)
  {
    return vm_.getModuleIndex(module);
  }
    
  int Compiler::findNative(gc<String> name)
  {
    return vm_.findNative(name);
  }

  void Compiler::declareModule(gc<ModuleAst> moduleAst, Module* module)
  {
    for (int i = 0; i < moduleAst->exprs().count(); i++)
    {
      DefExpr* def = moduleAst->exprs()[i]->asDefExpr();
      if (def != NULL)
      {
        gc<String> signature = SignatureBuilder::build(*def);
        
        declareMultimethod(signature);
      }
      
      // TODO(bob): Handle 'var' and 'defclass' here too.
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