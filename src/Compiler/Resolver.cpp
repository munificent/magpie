#include "Ast.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Module.h"
#include "Resolver.h"
#include "VM.h"

namespace magpie
{
  void Resolver::resolveBody(Compiler& compiler, Module& module, gc<Expr> body,
                             int& maxLocals, int& numClosures)
  {
    // Make a fake procedure so we can track closures.
    ResolvedProcedure procedure;
    maxLocals = resolve(compiler, module, NULL, &procedure, true, NULL, NULL,
                        NULL, body);
    numClosures = procedure.closures().count();
  }
  
  void Resolver::resolve(Compiler& compiler, Module& module, DefExpr& method)
  {
    resolve(compiler, module, NULL, &method.resolved(), false,
            method.leftParam(), method.rightParam(), method.value(),
            method.body());
  }

  int Resolver::resolve(Compiler& compiler, Module& module, Resolver* parent,
                        ResolvedProcedure* procedure, bool isModuleBody,
                        gc<Pattern> leftParam, gc<Pattern> rightParam,
                        gc<Pattern> valueParam, gc<Expr> body)
  {
    Resolver resolver(compiler, module, parent, isModuleBody);

    // Create a scope for the body.
    Scope scope(&resolver);
    resolver.scope_ = &scope;

    // First, we allocate slots for the destructured parameters. We do this
    // first so that all parameter slots for the method are contiguous at the
    // beginning of the method's slot window. The caller will assume this when
    // it sets up the arguments before the call.
    resolver.allocateSlotsForParam(leftParam);
    resolver.allocateSlotsForParam(rightParam);
    resolver.allocateSlotsForParam(valueParam);

    // Create a slot for the result value.
    resolver.makeLocal(new SourcePos(NULL, 0, 0, 0, 0),
                       String::create("(result)"));

    // Now that we've got our slots set up, we can actually resolve the nested
    // patterns for the param (if there are any).
    resolver.destructureParam(leftParam);
    resolver.destructureParam(rightParam);
    resolver.destructureParam(valueParam);

    resolver.resolve(body);

    scope.end();

    // TODO(bob): Copying this stuff here is lame.
    procedure->resolve(resolver.maxLocals_, resolver.closures_);

    return resolver.maxLocals_;
  }

  Resolver::Resolver(Compiler& compiler, Module& module, Resolver* parent,
                     bool isModuleBody)
  : compiler_(compiler),
    module_(module),
    parent_(parent),
    isModuleBody_(isModuleBody),
    locals_(),
    maxLocals_(0),
    closures_(),
    unnamedSlotId_(0),
    scope_(NULL),
    numLoops_(0)
  {}

  void Resolver::allocateSlotsForParam(gc<Pattern> pattern)
  {
    // No parameter so do nothing.
    if (pattern.isNull()) return;
    
    RecordPattern* record = pattern->asRecordPattern();
    if (record != NULL)
    {
      // Allocate each field.
      for (int i = 0; i < record->fields().count(); i++)
      {
        makeParamSlot(record->fields()[i].value);
      }
    }
    else
    {
      // If we got here, the pattern isn't a record, so it's a single slot.
      makeParamSlot(pattern);
    }
  }
  
  void Resolver::makeParamSlot(gc<Pattern> param)
  {
    VariablePattern* variable = param->asVariablePattern();
    if (variable != NULL)
    {
      // It's a variable, so create a named local for it and resolve the
      // variable.
      variable->setResolved(makeLocal(param->pos(), variable->name()));
      
      // Note that we do *not* resolve the variable's inner pattern here. We
      // do that after all param slots are resolved so that we can ensure the
      // param slots are contiguous.
    }
    else
    {
      // We don't have a variable for this parameter, but the argument
      // will still be on the stack, so make an unnamed slot for it.
      makeLocal(param->pos(), String::format("(%d)", unnamedSlotId_++));
    }
  }
  
  void Resolver::destructureParam(gc<Pattern> pattern)
  {
    // No parameter so do nothing.
    if (pattern.isNull()) return;
    
    RecordPattern* record = pattern->asRecordPattern();
    if (record != NULL)
    {
      // Resolve each field.
      for (int i = 0; i < record->fields().count(); i++)
      {
        resolveParam(record->fields()[i].value);
      }
    }
    else
    {
      // If we got here, the pattern isn't a record, so its a single slot.
      resolveParam(pattern);
    }
  }
  
  void Resolver::resolveParam(gc<Pattern> param)
  {
    VariablePattern* variable = param->asVariablePattern();
    if (variable != NULL)
    {
      // It's a variable, so resolve its inner pattern.
      if (!variable->pattern().isNull())
      {
        scope_->resolve(*variable->pattern());
      }
    }
    else
    {
      // Not a variable, so just resolve it normally.
      scope_->resolve(*param);
    }
  }
  
  void Resolver::resolve(gc<Expr> expr)
  {
    expr->accept(*this, -1);
  }
  
  void Resolver::resolveCall(CallExpr& expr, bool isLValue)
  {
    // Resolve the arguments.
    if (!expr.leftArg().isNull()) resolve(expr.leftArg());
    if (!expr.rightArg().isNull()) resolve(expr.rightArg());
    
    // Resolve the method.
    gc<String> signature = SignatureBuilder::build(expr, isLValue);
    int method = compiler_.findMethod(signature);
    
    if (method == -1)
    {
      compiler_.reporter().error(expr.pos(),
          "Could not find a method with signature '%s'.",
          signature->cString());
      
      // Just pick a method so we can keep compiling to report later errors.
      method = 0;
    }
    
    expr.setResolved(method);
  }

  int Resolver::resolveClosure(gc<String> name)
  {
    // If we walked all the way up the enclosing definitions and didn't find it,
    // then give up.
    if (parent_ == NULL) return -1;

    int parentClosure;
    
    gc<ResolvedName> outer = parent_->findLocal(name);
    if (outer.isNull())
    {
      // Couldn't find it in the parent procedure, so recurse upwards.
      parentClosure = parent_->resolveClosure(name);
      if (parentClosure == -1) return -1;
    }
    else
    {
      // Since we're closing over this, make sure the procedure where it's
      // defined knows it's a closure.
      if (outer->scope() == NAME_LOCAL)
      {
        // TODO(bob): When we transform this local into a closure, we don't
        // eliminate its slot on the stack, even though this means it doesn't
        // get used. Ideally, we should.
        // It's not a closure in the parent yet, so make it one.
        parentClosure = parent_->closures_.count();
        outer->makeClosure(parentClosure);
        parent_->closures_.add(-1);
      }
      else
      {
        ASSERT(outer->scope() == NAME_CLOSURE,
               "Local variable should be a closure.");
        parentClosure = outer->index();
      }
    }

    // If we got here, we've found the variable, and the parent procedure has
    // captured it in a closure. Now we can just capture that closure in our
    // scope.

    // See if we've got it already.
    int closure = closures_.indexOf(parentClosure);

    // If we don't already have a closure, create one.
    if (closure == -1)
    {
      closure = closures_.count();
      closures_.add(parentClosure);
    }

    return closure;
  }

  bool Resolver::resolveTopLevelName(Module& module, NameExpr& expr)
  {
    // Do not look up private names in other modules.
    if ((*expr.name())[0] == '_' && &module != &module_) return false;

    for (int i = 0; i < module.numVariables(); i++)
    {
      if (*module.getVariableName(i) == *expr.name())
      {
        // Found it.

        // Get the module's real index.
        int moduleIndex = compiler_.getModuleIndex(module);
        expr.setResolved(new ResolvedName(moduleIndex, i));
        return true;
      }
    }

    return false;
  }

  gc<ResolvedName> Resolver::findLocal(gc<String> name)
  {
    for (int i = locals_.count() - 1; i >= 0; i--)
    {
      if (locals_[i].name() == name) return locals_[i].resolved();
    }

    return NULL;
  }

  gc<ResolvedName> Resolver::makeLocal(gc<SourcePos> pos, gc<String> name)
  {
    // Make sure there isn't already a local variable with this name in the
    // current scope.
    for (int i = scope_->startSlot(); i < locals_.count(); i++)
    {
      if (locals_[i].name() == name)
      {
        compiler_.reporter().error(pos,
            "There is already a variable '%s' defined in this scope.",
            name->cString());
      }
    }

    gc<ResolvedName> resolved = new ResolvedName(locals_.count());
    locals_.add(Local(name, resolved));
    if (locals_.count() > maxLocals_) {
      maxLocals_ = locals_.count();
    }
            
    return resolved;
  }
  
  void Resolver::visit(AndExpr& expr, int dummy)
  {
    resolve(expr.left());
    resolve(expr.right());
  }

  void Resolver::visit(AssignExpr& expr, int dummy)
  {
    resolve(expr.value());
    expr.lvalue()->accept(*this, dummy);
  }

  void Resolver::visit(AsyncExpr& expr, int dummy)
  {
    resolve(compiler_, module_, this, &expr.resolved(), false, NULL, NULL,
            NULL, expr.body());
  }
  
  void Resolver::visit(BoolExpr& expr, int dummy)
  {
    // Do nothing.
  }

  void Resolver::visit(BreakExpr& expr, int dummy)
  {
    if (numLoops_ == 0)
    {
      compiler_.reporter().error(expr.pos(),
          "A 'break' can only appear within the body of a loop.");
    }
  }

  void Resolver::visit(CallExpr& expr, int dummy)
  {
    resolveCall(expr, false);
  }
  
  void Resolver::visit(CatchExpr& expr, int dummy)
  {
    // Resolve the block body.
    Scope tryScope(this);
    resolve(expr.body());
    tryScope.end();

    // Resolve the catch handlers.
    for (int i = 0; i < expr.catches().count(); i++)
    {
      Scope caseScope(this);
      const MatchClause& clause = expr.catches()[i];
      
      // Resolve the pattern.
      scope_->resolve(*clause.pattern());
      
      // Resolve the body.
      resolve(clause.body());
      caseScope.end();
    }
  }

  void Resolver::visit(CharacterExpr& expr, int dummy)
  {
    // Do nothing.
  }

  void Resolver::visit(DefExpr& expr, int dummy)
  {
    // Resolve the method itself.
    resolve(compiler_, module_, expr);
  }
  
  void Resolver::visit(DefClassExpr& expr, int dummy)
  {
    // Resolve the class name's variable.
    int module = compiler_.getModuleIndex(module_);
    int index = module_.findVariable(expr.name());
    
    ASSERT(index != -1, "Should have already forward-declared the class.");
    
    expr.setResolved(new ResolvedName(module, index));

    // Resolve the superclasses.
    for (int i = 0; i < expr.superclasses().count(); i++)
    {
      resolve(expr.superclasses()[i]);
    }
    
    // Resolve the synthesized stuff.
    for (int i = 0; i < expr.synthesizedMethods().count(); i++)
    {
      Resolver::resolve(compiler_, module_, *expr.synthesizedMethods()[i]);
    }
  }
  
  void Resolver::visit(DoExpr& expr, int dummy)
  {
    Scope doScope(this);
    resolve(expr.body());
    doScope.end();
  }

  void Resolver::visit(FloatExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(FnExpr& expr, int dummy)
  {
    // Resolve the function itself.
    resolve(compiler_, module_, this, &expr.resolved(), false,
            NULL, expr.pattern(), NULL, expr.body());
  }
  
  void Resolver::visit(ForExpr& expr, int dummy)
  {
    // Resolve the iterator in its own scope.
    Scope iteratorScope(this);
    resolve(expr.iterator());
    iteratorScope.end();

    // Resolve the body (including the loop pattern) in its own scope.
    Scope loopScope(this);
    
    scope_->resolve(*expr.pattern());
    
    numLoops_++;
    resolve(expr.body());
    numLoops_--;

    loopScope.end();
  }

  void Resolver::visit(GetFieldExpr& expr, int dummy)
  {
    // TODO(bob): This really shouldn't be an AST node. It should just be part
    // of some IR.
    // Do nothing.
  }
  
  void Resolver::visit(IfExpr& expr, int dummy)
  {
    Scope ifScope(this);

    // Resolve the condition.
    resolve(expr.condition());

    // Resolve the then arm.
    Scope thenScope(this);
    resolve(expr.thenArm());
    thenScope.end();

    if (!expr.elseArm().isNull())
    {
      Scope elseScope(this);
      resolve(expr.elseArm());
      elseScope.end();
    }

    ifScope.end();
  }

  void Resolver::visit(ImportExpr& expr, int dummy)
  {
    // Do nothing.
  }

  void Resolver::visit(IntExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(IsExpr& expr, int dummy)
  {
    resolve(expr.value());
    resolve(expr.type());
  }
  
  void Resolver::visit(ListExpr& expr, int dummy)
  {
    for (int i = 0; i < expr.elements().count(); i++)
    {
      resolve(expr.elements()[i]);
    }
  }
  
  void Resolver::visit(MatchExpr& expr, int dummy)
  {
    // Resolve the value.
    resolve(expr.value());

    // Resolve each case.
    for (int i = 0; i < expr.cases().count(); i++)
    {
      Scope caseScope(this);
      const MatchClause& clause = expr.cases()[i];
      
      // Resolve the pattern (will be null for the else case).
      if (!clause.pattern().isNull())
      {
        scope_->resolve(*clause.pattern());
      }

      // Resolve the body.
      resolve(clause.body());
      caseScope.end();
    }
  }

  void Resolver::visit(NameExpr& expr, int dummy)
  {
    // See if it's defined in this scope.
    gc<ResolvedName> local = findLocal(expr.name());
    if (!local.isNull())
    {
      expr.setResolved(local);
      return;
    }

    // See if it's a closure.
    int closure = resolveClosure(expr.name());
    if (closure != -1)
    {
      gc<ResolvedName> resolved = new ResolvedName(-1);
      resolved->makeClosure(closure);
      expr.setResolved(resolved);
      return;
    }

    // See if it's a top-level name in this module.
    if (resolveTopLevelName(module_, expr)) return;

    // See if it's an imported name. Walk through the modules this one imports.
    // TODO(bob): Need to handle name collisions.
    for (int i = 0; i < module_.imports().count(); i++)
    {
      Module* import = module_.imports()[i];
      if (resolveTopLevelName(*import, expr)) return;
    }

    compiler_.reporter().error(expr.pos(),
        "Variable '%s' is not defined.", expr.name()->cString());
    
    // Resolve it to some fake local so compilation can continue and report
    // more errors.
    expr.setResolved(new ResolvedName(0));
  }
  
  void Resolver::visit(NativeExpr& expr, int dummy)
  {
    int index = compiler_.findNative(expr.name());
    
    if (index == -1)
    {
      compiler_.reporter().error(expr.pos(),
          "Unknown native '%s'.", expr.name()->cString());
    }

    expr.setIndex(index);
  }
  
  void Resolver::visit(NotExpr& expr, int dummy)
  {
    resolve(expr.value());
  }
  
  void Resolver::visit(NothingExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(OrExpr& expr, int dummy)
  {
    resolve(expr.left());
    resolve(expr.right());
  }
  
  void Resolver::visit(RecordExpr& expr, int dummy)
  {
    // Resolve the fields.
    for (int i = 0; i < expr.fields().count(); i++)
    {
      resolve(expr.fields()[i].value);
    }
  }
  
  void Resolver::visit(ReturnExpr& expr, int dummy)
  {
    // Resolve the return value.
    if (!expr.value().isNull())
    {
      resolve(expr.value());
    }
  }
  
  void Resolver::visit(SequenceExpr& expr, int dummy)
  {
    for (int i = 0; i < expr.expressions().count(); i++)
    {
      resolve(expr.expressions()[i]);
    }
  }

  void Resolver::visit(SetFieldExpr& expr, int dummy)
  {
    // TODO(bob): This really shouldn't be an AST node. It should just be part
    // of some IR.
    // Do nothing.
  }
  
  void Resolver::visit(StringExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(ThrowExpr& expr, int dummy)
  {
    // Resolve the error object.
    resolve(expr.value());
  }
  
  void Resolver::visit(VariableExpr& expr, int dummy)
  {
    // Resolve the value.
    resolve(expr.value());
    
    // Now declare any locals on the left-hand side.
    scope_->resolve(*expr.pattern());
  }
  
  void Resolver::visit(WhileExpr& expr, int dest)
  {
    Scope loopScope(this);
    
    resolve(expr.condition());
    
    // TODO(bob): Should the body get its own scope?
    numLoops_++;
    resolve(expr.body());
    numLoops_--;
    
    loopScope.end();
  }
  
  void Resolver::visit(CallLValue& lvalue, int dummy)
  {
    resolveCall(*lvalue.call(), true);
  }
  
  void Resolver::visit(NameLValue& lvalue, int dummy)
  {
    // Look up the variable in this procedure.
    gc<ResolvedName> resolved = findLocal(lvalue.name());

    // TODO(bob): Report error if variable is immutable.

    if (!resolved.isNull())
    {
      lvalue.setResolved(resolved);
      return;
    }

    // See if it's a closure.
    int closure = resolveClosure(lvalue.name());
    if (closure != -1)
    {
      // TODO(bob): Report error if variable is immutable.
      gc<ResolvedName> resolved = new ResolvedName(-1);
      resolved->makeClosure(closure);
      lvalue.setResolved(resolved);
      return;
    }

    // Not a local variable. See if it's a top-level one.
    int module = compiler_.getModuleIndex(module_);
    int index = module_.findVariable(lvalue.name());
    
    if (index != -1)
    {
      lvalue.setResolved(new ResolvedName(module, index));
      return;
    }

    compiler_.reporter().error(lvalue.pos(),
        "Variable '%s' is not defined.", lvalue.name()->cString());
      
    // Put a fake slot in so we can continue and report more errors.
    lvalue.setResolved(new ResolvedName(0));
  }
  
  void Resolver::visit(RecordLValue& lvalue, int dummy)
  {
    // Recurse into the fields.
    for (int i = 0; i < lvalue.fields().count(); i++)
    {
      lvalue.fields()[i].value->accept(*this, dummy);
    }
  }
  
  void Resolver::visit(WildcardLValue& lvalue, int dummy)
  {
    // Nothing to do.
  }
    
  Scope::Scope(Resolver* resolver)
  : resolver_(*resolver),
    parent_(resolver_.scope_),
    start_(resolver_.locals_.count())
  {
    resolver_.scope_ = this;
  }
  
  Scope::~Scope()
  {
    ASSERT(start_ == -1, "Forgot to end scope.");
  }
  
  bool Scope::isTopLevel() const
  {
    return resolver_.isModuleBody_ && (parent_ == NULL);
  }

  void Scope::resolve(Pattern& pattern)
  {
    pattern.accept(*this, -1);
  }
  
  void Scope::end()
  {
    ASSERT(start_ != -1, "Already ended this scope.");
    
    resolver_.locals_.truncate(start_);
    resolver_.scope_ = parent_;
    start_ = -1;
  }
  
  void Scope::visit(RecordPattern& pattern, int dummy)
  {
    // Recurse into the fields.
    for (int i = 0; i < pattern.fields().count(); i++)
    {
      pattern.fields()[i].value->accept(*this, dummy);
    }
  }
  
  void Scope::visit(TypePattern& pattern, int dummy)
  {
    // Resolve the type expression.
    resolver_.resolve(pattern.type());
  }
  
  void Scope::visit(ValuePattern& pattern, int dummy)
  {
    // Resolve the value expression.
    resolver_.resolve(pattern.value());
  }
  
  void Scope::visit(VariablePattern& pattern, int dummy)
  {
    if (isTopLevel())
    {
      // It's a top-level module variable. Since these are forward declared,
      // they should already exist. Just look up the existing one.
      int module = resolver_.compiler_.getModuleIndex(resolver_.module_);
      int index = resolver_.module_.findVariable(pattern.name());
      
      if (index == -1)
      {
        resolver_.compiler_.reporter().error(pattern.pos(),
            "Variable '%s' is not defined.", pattern.name()->cString());
        
        // Put a fake index in so we can continue and report more errors.
        index = 0;
      }
      
      pattern.setResolved(new ResolvedName(module, index));
    }
    else
    {
      // Declaring a local variable, so create a slot for it.
      gc<ResolvedName> resolved = resolver_.makeLocal(pattern.pos(),
                                                      pattern.name());
      pattern.setResolved(resolved);
    }
        
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, dummy);
    }
  }
  
  void Scope::visit(WildcardPattern& pattern, int dummy)
  {
    // Nothing to do.
  }
}