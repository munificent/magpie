#pragma once

#include "Array.h"
#include "Memory.h"
#include "Ast.h"

namespace magpie
{
  class Compiler;
  class Scope;
  class VM;
  
  class Resolver : public ExprVisitor
  {
    friend class Scope;
    
  public:
    static void resolve(Compiler& compiler, const Module& module,
                        MethodDef& method);
    
  private:
    Resolver(Compiler& compiler, const Module& module)
    : compiler_(compiler),
      module_(module),
      locals_(),
      maxLocals_(0),
      unnamedSlotId_(0),
      scope_(NULL)
    {}
    
    // The AST for a method parameter is a single pattern on each side, but the
    // compile implicitly destructures it when the pattern is a record so that
    // we don't allocate a record only to immediately destructure it. These
    // methods methods handle that one-level-deep destructuring.
    void allocateSlotsForParam(gc<Pattern> pattern);
    void makeParamSlot(gc<Pattern> param);
    void destructureParam(gc<Pattern> pattern);
    void resolveParam(gc<Pattern> pattern);
    void resolve(gc<Expr> expr);
    
    // Creates a new local variable with the given name.
    int makeLocal(const SourcePos& pos, gc<String> name);
        
    virtual void visit(AndExpr& expr, int dummy);
    virtual void visit(AssignExpr& expr, int dest);
    virtual void visit(BinaryOpExpr& expr, int dummy);
    virtual void visit(BoolExpr& expr, int dummy);
    virtual void visit(CallExpr& expr, int dummy);
    virtual void visit(CatchExpr& expr, int dummy);
    virtual void visit(DoExpr& expr, int dummy);
    virtual void visit(IfExpr& expr, int dummy);
    virtual void visit(IsExpr& expr, int dummy);
    virtual void visit(MatchExpr& expr, int dummy);
    virtual void visit(NameExpr& expr, int dummy);
    virtual void visit(NativeExpr& expr, int dest);
    virtual void visit(NotExpr& expr, int dummy);
    virtual void visit(NothingExpr& expr, int dummy);
    virtual void visit(NumberExpr& expr, int dummy);
    virtual void visit(OrExpr& expr, int dummy);
    virtual void visit(RecordExpr& expr, int dummy);
    virtual void visit(ReturnExpr& expr, int dummy);
    virtual void visit(SequenceExpr& expr, int dummy);
    virtual void visit(StringExpr& expr, int dummy);
    virtual void visit(ThrowExpr& expr, int dummy);
    virtual void visit(VariableExpr& expr, int dummy);
    virtual void visit(WhileExpr& expr, int dummy);

    Compiler& compiler_;

    const Module& module_;
    
    // The names of the current in-scope local variables (including all outer
    // scopes). The indices in this array correspond to the slots where those
    // locals are stored.
    Array<gc<String> > locals_;
    
    // The maximum number of locals that are in scope at the same time.
    int maxLocals_;

    // We sometimes need to create placeholder locals to make sure the indices
    // in locals_ line up with slots. This is used to name them.
    int unnamedSlotId_;
    
    // The current inner-most local variable scope.
    Scope* scope_;
    
    NO_COPY(Resolver);
  };
  
  // Keeps track of local variable scopes to handle nesting and shadowing.
  // This class can also visit patterns in order to create slots for the
  // variables declared in a pattern.
  class Scope : private PatternVisitor
  {
  public:
    Scope(Resolver* resolver);      
    ~Scope();
    
    void resolve(Pattern& pattern);
    void resolveAssignment(Pattern& pattern);
    
    int startSlot() const { return start_; }
    
    // Closes this scope. This must be called before the Scope object goes out
    // of (C++) scope.
    void end();
    
    virtual void visit(RecordPattern& pattern, int isAssignment);
    virtual void visit(TypePattern& pattern, int isAssignment);
    virtual void visit(ValuePattern& pattern, int isAssignment);
    virtual void visit(VariablePattern& pattern, int isAssignment);
    virtual void visit(WildcardPattern& pattern, int isAssignment);
    
  private:
    Resolver& resolver_;
    Scope* parent_;
    int start_;
    
    NO_COPY(Scope);
  };
}