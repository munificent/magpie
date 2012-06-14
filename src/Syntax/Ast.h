#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"
#include "Token.h"

namespace magpie
{
  using std::ostream;

  class Def;
  class Expr;
  class Pattern;
  
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
  
  // A clause that gets executed every iteration in a loop: either a "while",
  // or a "for".
  class LoopClause
  {
  public:
    LoopClause()
    : pattern_(),
      expression_()
    {}
    
    LoopClause(gc<Expr> condition)
    : isFor_(false),
      pattern_(),
      expression_(condition)
    {}
    
    LoopClause(gc<Pattern> pattern, gc<Expr> expression)
    : isFor_(true),
      pattern_(pattern),
      expression_(expression)
    {}
    
    bool isFor() const { return isFor_; }
    gc<Pattern> pattern() const { return pattern_; }
    gc<Expr> expression() const { return expression_; }    
  
  private:
    bool isFor_;
    gc<Pattern> pattern_;
    gc<Expr> expression_;
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
      import_(-1),
      index_(-1)
    {}
    
    // Resolves the name to a local variable with the given slot index.
    ResolvedName(int index)
    : isLocal_(true),
      import_(-1),
      index_(index)
    {}
    
    // Resolves the name to a variable that is the given export from the given
    // import.
    ResolvedName(int importIndex, int exportIndex)
    : isLocal_(false),
      import_(importIndex),
      index_(exportIndex)
    {}
    
    bool isResolved() const { return index_ != -1; }
    bool isLocal() const { return isLocal_; }
    int import() const { return import_; }
    int index() const { return index_; }
    
  private:
    bool isLocal_;
    int import_;
    int index_;
  };
  
  class ModuleAst : public Managed
  {
  public:
    ModuleAst(const Array<gc<Def> >& defs, gc<Expr> body)
    : defs_(defs),
      body_(body)
    {}
    
    const Array<gc<Def> >& defs() const { return defs_; }
    gc<Expr> body() const { return body_; }
    
    virtual void reach()
    {
      Memory::reach(defs_);
      Memory::reach(body_);
    }
    
  private:
    Array<gc<Def> > defs_;
    gc<Expr>        body_;
  };
  
#include "Ast.generated.h"
}

