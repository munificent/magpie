#include "Ast.h"
#include "ErrorReporter.h"
#include "Module.h"
#include "Resolver.h"

namespace magpie
{
  void Resolver::resolve(ErrorReporter& reporter, const Module& module,
                         MethodDef& method)
  {
    Resolver resolver(reporter, module);

    // Create a top-level scope.
    Scope2 scope(&resolver);
    resolver.scope_ = &scope;
    
    // TODO(bob): This should go away when we're handling the args correctly.
    // Create a fake local for the argument and result value.
    scope.makeLocal(method.pos(), String::create("(return)"));
    
    // Create variables for the parameters.
    if (!method.leftParam().isNull())
    {
      scope.resolve(*method.leftParam());
    }

    if (!method.rightParam().isNull())
    {
      scope.resolve(*method.rightParam());
    }
    
    resolver.resolve(method.body());
    
    scope.end();
  }
  
  void Resolver::resolve(gc<Expr> expr)
  {
    expr->accept(*this, -1);
  }

  void Resolver::visit(AndExpr& expr, int dummy)
  {
    expr.left()->accept(*this, dummy);
    expr.right()->accept(*this, dummy);
  }
  
  void Resolver::visit(BinaryOpExpr& expr, int dummy)
  {
    expr.left()->accept(*this, dummy);
    expr.right()->accept(*this, dummy);
  }
  
  void Resolver::visit(BoolExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(CallExpr& expr, int dummy)
  {
    if (!expr.leftArg().isNull())
    {
      expr.leftArg()->accept(*this, dummy);
    }

    // TODO(bob): Resolve method here too?
    
    if (!expr.rightArg().isNull())
    {
      expr.rightArg()->accept(*this, dummy);
    }
  }
  
  void Resolver::visit(CatchExpr& expr, int dummy)
  {
    // Resolve the block body.
    Scope2 tryScope(this);
    expr.body()->accept(*this, dummy);
    tryScope.end();

    // Resolve the catch handlers.
    for (int i = 0; i < expr.catches().count(); i++)
    {
      Scope2 caseScope(this);
      const MatchClause& clause = expr.catches()[i];
      
      // Resolve the pattern.
      scope_->resolve(*clause.pattern());
      
      // Resolve the body.
      clause.body()->accept(*this, dummy);
      caseScope.end();
    }
  }
  
  void Resolver::visit(DoExpr& expr, int dummy)
  {
    Scope2 doScope(this);
    expr.body()->accept(*this, dummy);
    doScope.end();
  }
  
  void Resolver::visit(IfExpr& expr, int dummy)
  {
    Scope2 ifScope(this);

    // Resolve the condition.
    expr.condition()->accept(*this, dummy);

    // Resolve the then arm.
    Scope2 thenScope(this);
    expr.thenArm()->accept(*this, dummy);
    thenScope.end();

    if (!expr.elseArm().isNull())
    {
      Scope2 elseScope(this);
      expr.elseArm()->accept(*this, dummy);
      elseScope.end();
    }

    ifScope.end();
  }
  
  void Resolver::visit(IsExpr& expr, int dummy)
  {
    expr.value()->accept(*this, dummy);
    expr.type()->accept(*this, dummy);
  }
  
  void Resolver::visit(MatchExpr& expr, int dummy)
  {
    // Resolve the value.
    expr.value()->accept(*this, dummy);

    // Resolve each case.
    for (int i = 0; i < expr.cases().count(); i++)
    {
      Scope2 caseScope(this);
      const MatchClause& clause = expr.cases()[i];
      
      // Resolve the pattern.
      scope_->resolve(*clause.pattern());

      // Resolve the body.
      clause.body()->accept(*this, dummy);
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
          expr.setResolved(ResolvedName(i, j));
          return;
        }
      }
    }

    reporter_.error(expr.pos(),
        "Variable '%s' is not defined.", expr.name()->cString());
    
    // Resolve it to some fake local so compilation can continue and report
    // more errors.
    expr.setResolved(ResolvedName(0));
  }
  
  void Resolver::visit(NotExpr& expr, int dummy)
  {
    expr.value()->accept(*this, dummy);
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
    expr.left()->accept(*this, dummy);
    expr.right()->accept(*this, dummy);
  }
  
  void Resolver::visit(RecordExpr& expr, int dummy)
  {
    // Resolve the fields.
    for (int i = 0; i < expr.fields().count(); i++)
    {
      expr.fields()[i].value->accept(*this, dummy);
    }
  }
  
  void Resolver::visit(ReturnExpr& expr, int dummy)
  {
    // Resolve the return value.
    if (!expr.value().isNull())
    {
      expr.value()->accept(*this, dummy);
    }
  }
  
  void Resolver::visit(SequenceExpr& expr, int dummy)
  {
    for (int i = 0; i < expr.expressions().count(); i++)
    {
      expr.expressions()[i]->accept(*this, dummy);
    }
  }
  
  void Resolver::visit(StringExpr& expr, int dummy)
  {
    // Do nothing.
  }
  
  void Resolver::visit(ThrowExpr& expr, int dummy)
  {
    // Resolve the error object.
    expr.value()->accept(*this, dummy);
  }
  
  void Resolver::visit(VariableExpr& expr, int dummy)
  {
    // Resolve the value.
    expr.value()->accept(*this, dummy);
    
    // Now declare any locals on the left-hand side.
    scope_->resolve(*expr.pattern());
  }
  
  Scope2::Scope2(Resolver* resolver)
  : resolver_(*resolver),
    parent_(resolver_.scope_),
    start_(resolver_.locals_.count())
  {
    resolver_.scope_ = this;
  }
  
  Scope2::~Scope2()
  {
    ASSERT(start_ == -1, "Forgot to end scope.");
  }
  
  void Scope2::resolve(Pattern& pattern)
  {
    pattern.accept(*this, -1);
  }

  int Scope2::makeLocal(const SourcePos& pos, gc<String> name)
  {
    Array<gc<String> >& locals = resolver_.locals_;
    
    // Make sure there isn't already a local variable with this name in this
    // scope.
    for (int i = start_; i < locals.count(); i++)
    {
      if (locals[i] == name)
      {
        resolver_.reporter_.error(pos,
            "There is already a variable '%s' defined in this scope.",
            name->cString());
      }
    }
    
    resolver_.locals_.add(name);
    /*
    resolver_.updateMaxRegisters();
    */
    return resolver_.locals_.count() - 1;
  }
  
  void Scope2::end()
  {
    ASSERT(start_ != -1, "Already ended this scope.");

    resolver_.locals_.truncate(start_);
    resolver_.scope_ = parent_;
    start_ = -1;
  }
  
  void Scope2::visit(RecordPattern& pattern, int dummy)
  {
    // Recurse into the fields.
    for (int i = 0; i < pattern.fields().count(); i++)
    {
      pattern.fields()[i].value->accept(*this, dummy);
    }
  }
  
  void Scope2::visit(TypePattern& pattern, int value)
  {
    // Resolve the type expression.
    resolver_.resolve(pattern.type());
  }
  
  void Scope2::visit(ValuePattern& pattern, int dummy)
  {
    // Resolve the value expression.
    resolver_.resolve(pattern.value());
  }
  
  void Scope2::visit(VariablePattern& pattern, int dummy)
  {
    makeLocal(pattern.pos(), pattern.name());
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, dummy);
    }
  }
  
  void Scope2::visit(WildcardPattern& pattern, int dummy)
  {
    // Nothing to do.
  }
}