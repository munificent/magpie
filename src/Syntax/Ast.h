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

  class ResolvedName
  {
  public:
    // Creates a new unresolved name.
    ResolvedName()
    : scope_(NAME_LOCAL),
      module_(-1),
      index_(-1)
    {}
    
    // Resolves the name to a local variable with the given slot index.
    ResolvedName(int index)
    : scope_(NAME_LOCAL),
      module_(-1),
      index_(index)
    {}

    // Resolves the name to a variable with the given scope and slot index.
    ResolvedName(NameScope scope, int index)
    : scope_(scope),
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
    
  private:
    NameScope scope_;
    int module_;
    int index_;
  };

  // A reference to a variable declared outside of the scope where it's used.
  class Closure
  {
  public:
    // Default constructor so we can use this in Arrays.
    Closure()
    : isLocal_(false),
    slot_(-1)
    {}

    Closure(bool isLocal, int slot)
    : isLocal_(isLocal),
    slot_(slot)
    {}

    // True if this is capturing a local variable from the enclosing scope.
    // False if it's capturing an upvar from the enclosing scope.
    bool isLocal() const { return isLocal_; }
    int slot() const { return slot_; }

  private:
    bool isLocal_;
    int  slot_;
  };

  // Contains the information generated during resolution for something containg
  // code: a method, function, or async block.
  class ResolvedProcedure
  {
  public:
    ResolvedProcedure()
    : closures_(),
      maxLocals_(-1)
    {}

    bool isResolved() const { return maxLocals_ != -1; }

    // The variables that this procedure accesses that are defined in outer
    // scopes. These will become [Upvar]s at runtime.
    Array<Closure>& closures() { return closures_; }
    
    int maxLocals() const { return maxLocals_; }
    void setMaxLocals(int maxLocals) { maxLocals_ = maxLocals; }

  private:
    Array<Closure> closures_;
    
    int maxLocals_;

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

