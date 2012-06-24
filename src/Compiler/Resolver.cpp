#include "Ast.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Module.h"
#include "Resolver.h"
#include "VM.h"

namespace magpie
{
  void Resolver::resolve(Compiler& compiler, const Module& module,
                         MethodDef& method)
  {
    Resolver resolver(compiler, module);

    // Create a top-level scope.
    Scope scope(&resolver);
    resolver.scope_ = &scope;
    
    // First, we allocate slots for the destructured parameters. We do this
    // first so that all parameter slots for the method are contiguous at the
    // beginning of the method's slot window. The caller will assume this when
    // it sets up the arguments before the call.
    resolver.allocateSlotsForParam(method.leftParam());
    resolver.allocateSlotsForParam(method.rightParam());
    
    // Create a slot for the result value.
    resolver.makeLocal(method.pos(), String::create("(result)"));
    
    // Now that we've got our slots set up, we can actually resolve the nested
    // patterns for the param (if there are any).
    resolver.destructureParam(method.leftParam());
    resolver.destructureParam(method.rightParam());
    
    resolver.resolve(method.body());
    
    scope.end();
    
    method.setMaxLocals(resolver.maxLocals_);
  }
  
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
      int slot = makeLocal(param->pos(), variable->name());
      variable->setResolved(ResolvedName(slot));
      
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
  
  int Resolver::makeLocal(const SourcePos& pos, gc<String> name)
  {
    // Make sure there isn't already a local variable with this name in the
    // current scope.
    for (int i = scope_->startSlot(); i < locals_.count(); i++)
    {
      if (locals_[i] == name)
      {
        compiler_.reporter().error(pos,
            "There is already a variable '%s' defined in this scope.",
            name->cString());
      }
    }
    
    locals_.add(name);
    if (locals_.count() > maxLocals_) {
      maxLocals_ = locals_.count();
    }
            
    return locals_.count() - 1;
  }
  
  void Resolver::visit(AndExpr& expr, int dummy)
  {
    resolve(expr.left());
    resolve(expr.right());
  }
  
  void Resolver::visit(AssignExpr& expr, int dest)
  {
    scope_->resolveAssignment(*expr.pattern());
    resolve(expr.value());
  }
  
  void Resolver::visit(BinaryOpExpr& expr, int dummy)
  {
    resolve(expr.left());
    resolve(expr.right());
  }
  
  void Resolver::visit(BoolExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(CallExpr& expr, int dummy)
  {
    if (!expr.leftArg().isNull())
    {
      resolve(expr.leftArg());
    }

    // TODO(bob): Resolve method here too?
    
    if (!expr.rightArg().isNull())
    {
      resolve(expr.rightArg());
    }
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
  
  void Resolver::visit(DoExpr& expr, int dummy)
  {
    Scope doScope(this);
    resolve(expr.body());
    doScope.end();
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
  
  void Resolver::visit(IsExpr& expr, int dummy)
  {
    resolve(expr.value());
    resolve(expr.type());
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
      
      // Resolve the pattern.
      scope_->resolve(*clause.pattern());

      // Resolve the body.
      resolve(clause.body());
      caseScope.end();
    }
  }
  
  void Resolver::visit(NameExpr& expr, int dummy)
  {
    // See if it's a local variable.
    int local = locals_.lastIndexOf(expr.name());
    if (local != -1)
    {
      expr.setResolved(ResolvedName(local));
      return;
    }

    // See if it's an imported name. Walk through the modules this one imports.
    // TODO(bob): Need to handle name collisions.
    for (int i = 0; i < module_.imports().count(); i++)
    {
      Module* import = module_.imports()[i];

      // Walk through the names it exports.
      for (int j = 0; j < import->numExports(); j++)
      {
        if (*import->getExportName(j) == *expr.name())
        {
          // Found it.
          
          // Get the module's real index.
          int module = compiler_.getModuleIndex(import);
          expr.setResolved(ResolvedName(module, j));
          return;
        }
      }
    }

    compiler_.reporter().error(expr.pos(),
        "Variable '%s' is not defined.", expr.name()->cString());
    
    // Resolve it to some fake local so compilation can continue and report
    // more errors.
    expr.setResolved(ResolvedName(0));
  }
  
  void Resolver::visit(NativeExpr& expr, int dummy)
  {
    // TODO(bob): Hack temp. Put this somewhere central.
    if (*expr.name() == "print") expr.setIndex(0);
    else if (*expr.name() == "num +") expr.setIndex(1);
    else if (*expr.name() == "num -") expr.setIndex(2);
    else if (*expr.name() == "num *") expr.setIndex(3);
    else if (*expr.name() == "num /") expr.setIndex(4);
    else
    {
      compiler_.reporter().error(expr.pos(),
          "Unknown native '%s'.", expr.name()->cString());
      expr.setIndex(0);
    }
  }
  
  void Resolver::visit(NotExpr& expr, int dummy)
  {
    resolve(expr.value());
  }
  
  void Resolver::visit(NothingExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(NumberExpr& expr, int dummy)
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
    resolve(expr.body());
    
    loopScope.end();
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
  
  void Scope::resolve(Pattern& pattern)
  {
    pattern.accept(*this, 0);
  }
  
  void Scope::resolveAssignment(Pattern& pattern)
  {
    pattern.accept(*this, 1);
  }
  
  void Scope::end()
  {
    ASSERT(start_ != -1, "Already ended this scope.");
    
    resolver_.locals_.truncate(start_);
    resolver_.scope_ = parent_;
    start_ = -1;
  }
  
  void Scope::visit(RecordPattern& pattern, int isAssignment)
  {
    // Recurse into the fields.
    for (int i = 0; i < pattern.fields().count(); i++)
    {
      pattern.fields()[i].value->accept(*this, isAssignment);
    }
  }
  
  void Scope::visit(TypePattern& pattern, int isAssignment)
  {
    // Resolve the type expression.
    resolver_.resolve(pattern.type());
  }
  
  void Scope::visit(ValuePattern& pattern, int isAssignment)
  {
    // Resolve the value expression.
    resolver_.resolve(pattern.value());
  }
  
  void Scope::visit(VariablePattern& pattern, int isAssignment)
  {
    int slot;
    if (isAssignment == 1)
    {
      // Assigning to an existing variable, so look it up.
      slot = resolver_.locals_.lastIndexOf(pattern.name());
      
      // TODO(bob): Report error if variable is immutable.
      
      if (slot == -1)
      {
        resolver_.compiler_.reporter().error(pattern.pos(),
            "Variable '%s' is not defined.", pattern.name()->cString());
        
        // Put a fake slot in so we can continue and report more errors.
        slot = 0;
      }
    }
    else
    {
      // Declaring a variable, so create a slot for it.
      slot = resolver_.makeLocal(pattern.pos(), pattern.name());
    }

    pattern.setResolved(ResolvedName(slot));
    
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, isAssignment);
    }
  }
  
  void Scope::visit(WildcardPattern& pattern, int isAssignment)
  {
    // Nothing to do.
  }
}