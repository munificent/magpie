#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"
#include "Token.h"

namespace magpie
{
  using std::ostream;

  class Expr;
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
  
  class ResolvedName
  {
  public:
    // Creates a new unresolved name.
    ResolvedName()
    : isLocal_(false),
      module_(-1),
      index_(-1)
    {}
    
    // Resolves the name to a local variable with the given slot index.
    ResolvedName(int index)
    : isLocal_(true),
      module_(-1),
      index_(index)
    {}
    
    // Resolves the name to a variable that is the given export from the given
    // import.
    ResolvedName(int module, int variable)
    : isLocal_(false),
      module_(module),
      index_(variable)
    {}
    
    bool isResolved() const { return index_ != -1; }
    bool isLocal() const { return isLocal_; }
    int module() const { return module_; }
    int index() const { return index_; }
    
  private:
    bool isLocal_;
    int module_;
    int index_;
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
      Memory::reach(body_);
    }
    
  private:
    gc<SequenceExpr> body_;
  };
  
#include "Ast.generated.h"
}

