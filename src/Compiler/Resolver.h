#pragma once

#include "Array.h"
#include "Memory.h"
#include "Ast.h"

namespace magpie
{
  class Compiler;
  class Scope;
  class VM;

  // Tracks information about a local variable.
  class Local
  {
  public:
    Local(gc<String> name, gc<ResolvedName> resolved)
    : name_(name),
      resolved_(resolved)
    {}

    // Default constructor so it can be used in Arrays.
    Local()
    {}

    gc<String> name() { return name_; }
    gc<ResolvedName> resolved() { return resolved_; }

  private:
    gc<String> name_;
    gc<ResolvedName> resolved_;
  };

  class Resolver : public ExprVisitor, private LValueVisitor
  {
    friend class Scope;

  public:
    static void resolveBody(Compiler& compiler, Module& module, gc<Expr> body,
                            int& maxLocals, int& numClosures);

  private:
    static void resolve(Compiler& compiler, Module& module, DefExpr& method);
    static int resolve(Compiler& compiler, Module& module, Resolver* parent,
                       ResolvedProcedure* procedure, bool isModuleBody,
                       gc<Pattern> leftParam, gc<Pattern> rightParam,
                       gc<Pattern> valueParam, gc<Expr> body);

    Resolver(Compiler& compiler, Module& module, Resolver* parent,
             bool isModuleBody)
    : compiler_(compiler),
      module_(module),
      parent_(parent),
      isModuleBody_(isModuleBody),
      locals_(),
      maxLocals_(0),
      closures_(),
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
    void resolveCall(CallExpr& expr, bool isLValue);

    // Attempts to resolve a name defined in a local variable scope. Returns
    // the index of the closure in this procedure if found, or -1 if the name
    // could not be resolved.
    int resolveClosure(gc<String> name);
    
    bool resolveTopLevelName(Module& module, NameExpr& expr);

    // Returns the resolved local variable with [name] or NULL if not found.
    gc<ResolvedName> findLocal(gc<String> name);

    // Creates a new local variable with the given name.
    gc<ResolvedName> makeLocal(const SourcePos& pos, gc<String> name);
        
    virtual void visit(AndExpr& expr, int dummy);
    virtual void visit(AssignExpr& expr, int dest);
    virtual void visit(AsyncExpr& expr, int dummy);
    virtual void visit(BinaryOpExpr& expr, int dummy);
    virtual void visit(BoolExpr& expr, int dummy);
    virtual void visit(CallExpr& expr, int dummy);
    virtual void visit(CatchExpr& expr, int dummy);
    virtual void visit(DefExpr& expr, int dummy);
    virtual void visit(DefClassExpr& expr, int dummy);
    virtual void visit(DoExpr& expr, int dummy);
    virtual void visit(FnExpr& expr, int dummy);
    virtual void visit(ForExpr& expr, int dummy);
    virtual void visit(GetFieldExpr& expr, int dummy);
    virtual void visit(IfExpr& expr, int dummy);
    virtual void visit(IsExpr& expr, int dummy);
    virtual void visit(ListExpr& expr, int dummy);
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
    virtual void visit(SetFieldExpr& expr, int dummy);
    virtual void visit(StringExpr& expr, int dummy);
    virtual void visit(ThrowExpr& expr, int dummy);
    virtual void visit(VariableExpr& expr, int dummy);
    virtual void visit(WhileExpr& expr, int dummy);

    virtual void visit(CallLValue& lvalue, int dummy);
    virtual void visit(NameLValue& lvalue, int dummy);
    virtual void visit(RecordLValue& lvalue, int dummy);
    virtual void visit(WildcardLValue& lvalue, int dummy);

    Compiler& compiler_;
    Module& module_;

    // If this resolver is resolving a function or async block, this will point
    // to the resolver for the containing expression. Used to resolve closures.
    Resolver* parent_;
    
    // True if this resolver is resolving top-level expressions in a module
    // body. False if it's resolving a method body.
    bool isModuleBody_;
    
    // The names of the current in-scope local variables (including all outer
    // scopes). The indices in this array correspond to the slots where those
    // locals are stored.
    Array<Local> locals_;
    
    // The maximum number of locals that are in scope at the same time.
    int maxLocals_;

    Array<int> closures_;

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
    
    bool isTopLevel() const;
    
    void resolve(Pattern& pattern);
    
    int startSlot() const { return start_; }
    
    // Closes this scope. This must be called before the Scope object goes out
    // of (C++) scope.
    void end();
    
    virtual void visit(RecordPattern& pattern, int dummy);
    virtual void visit(TypePattern& pattern, int dummy);
    virtual void visit(ValuePattern& pattern, int dummy);
    virtual void visit(VariablePattern& pattern, int dummy);
    virtual void visit(WildcardPattern& pattern, int dummy);
    
  private:
    Resolver& resolver_;
    Scope* parent_;
    int start_;
    
    NO_COPY(Scope);
  };
}