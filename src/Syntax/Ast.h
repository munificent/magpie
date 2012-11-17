#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"
#include "Token.h"

namespace magpie
{
  using std::ostream;

  class Expr;
  class LValue;
  class Module;
  class Pattern;
  class SequenceExpr;
  
  // A record field.
  struct Field
  {
    Field()
    : name(),
      value()
    {}
    
    Field(gc<String> name, gc<Expr> value)
    : name(name),
      value(value)
    {}

    gc<String> name;
    gc<Expr> value;
  };
  
  // A record pattern field.
  struct PatternField
  {
    PatternField()
    : name(),
      value()
    {}
    
    PatternField(gc<String> name, gc<Pattern> value)
    : name(name),
      value(value)
    {}
    
    gc<String> name;
    gc<Pattern> value;
  };

  // A record lvalue field.
  struct LValueField
  {
    LValueField()
    : name(),
      value()
    {}
    
    LValueField(gc<String> name, gc<LValue> value)
    : name(name),
      value(value)
    {}
    
    gc<String> name;
    gc<LValue> value;
  };
  
  // A class field.
  class ClassField : public Managed
  {
  public:
    ClassField(bool isMutable, gc<String> name, gc<Pattern> pattern,
               gc<Expr> initializer)
    : isMutable_(isMutable),
      name_(name),
      pattern_(pattern),
      initializer_(initializer)
    {}
    
    bool isMutable() const { return isMutable_; }
    gc<String> name() const { return name_; }
    gc<Pattern> pattern() const { return pattern_; }
    gc<Expr> initializer() const { return initializer_; }
    
    void reach();
    
  private:
    bool isMutable_;
    gc<String> name_;
    gc<Pattern> pattern_;
    gc<Expr> initializer_;
  };
  
  // A pattern paired with the expression to execute when the pattern matches.
  // Used for match expressions and catch clauses.
  class MatchClause
  {
  public:
    MatchClause()
    : pattern_(),
      body_()
    {}
    
    MatchClause(gc<Pattern> pattern, gc<Expr> body)
    : pattern_(pattern),
      body_(body)
    {}
    
    gc<Pattern> pattern() const { return pattern_; }
    gc<Expr> body() const { return body_; }
    
  private:
    gc<Pattern> pattern_;
    gc<Expr> body_;
  };

  enum NameScope
  {
    // The name is a local variable in the current definition.
    NAME_LOCAL,

    // The name is a local variable in an enclosing definition.
    NAME_CLOSURE,

    // The name is a top-level variable in the module.
    NAME_MODULE
  };

  class ResolvedName : public Managed
  {
  public:
    // Resolves the name to a local variable with the given slot index.
    ResolvedName(int index)
    : scope_(NAME_LOCAL),
      module_(-1),
      index_(index)
    {}

    // Resolves the name to a variable that is the given export from the given
    // import.
    ResolvedName(int module, int variable)
    : scope_(NAME_MODULE),
      module_(module),
      index_(variable)
    {}

    bool isResolved() const { return index_ != -1; }
    NameScope scope() const { return scope_; }
    int module() const { return module_; }
    int index() const { return index_; }

    // Marks this name as being a local variable that is accessed by an inner
    // definition.
    void makeClosure(int index) {
      scope_ = NAME_CLOSURE;
      index_ = index;
    }

  private:
    NameScope scope_;
    int module_;
    int index_;
  };

  // Contains the information generated during resolution for something containg
  // code: a method, function, or async block.
  class ResolvedProcedure
  {
  public:
    ResolvedProcedure()
    : maxLocals_(-1),
      closures_()
    {}

    bool isResolved() const { return maxLocals_ != -1; }

    int maxLocals() const { return maxLocals_; }

    // Every function has a list of "closures". These are variables that have
    // non-local extent, either because they are accessed from an enclosing
    // procedure or by an enclosed one. This keeps track of them. Each element
    // here is a closure in this procedure's scope.
    //
    // The value is the index of a closure in the outer scope that this one is
    // capturing. If the number is -1, that means this closure exists purely so
    // that an inner procedure can access it. From the perspective of this
    // procedure, it is created from scratch.
    const Array<int>& closures() const { return closures_; }

    void resolve(int maxLocals, const Array<int>& closures)
    {
      maxLocals_ = maxLocals;
      closures_.addAll(closures);
    }
    
  private:
    int maxLocals_;

    Array<int> closures_;

    NO_COPY(ResolvedProcedure);
  };

  class ModuleAst : public Managed
  {
  public:
    ModuleAst(gc<SequenceExpr> body)
    : body_(body)
    {}
    
    gc<SequenceExpr> body() const { return body_; }
    
    virtual void reach()
    {
      body_.reach();
    }
    
  private:
    gc<SequenceExpr> body_;
  };
  
#include "Ast.generated.h"
}

