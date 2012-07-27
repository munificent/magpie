#pragma once

#include "Array.h"
#include "Ast.h"
#include "Compiler.h"
#include "Memory.h"

namespace magpie
{
  class Module;
  
  class MethodCompiler : private ExprVisitor
  {
    friend class Compiler;
    friend class PatternCompiler;
    
  public:
    virtual ~MethodCompiler() {}
    
  private:    
    MethodCompiler(Compiler& compiler, Module* module);
    
    gc<Chunk> compileBody(gc<Expr> body);

    // TODO(bob): Hack temp. This is just to get single non-multimethods working
    // with the new deferred compilation model.
    gc<Chunk> compileTemp(DefExpr& method);
    
    void compileParam(gc<Pattern> param, int& slot);
    void compileParamField(gc<Pattern> param, int slot);
    int compileArg(gc<Expr> arg);
    
    void compile(gc<Expr> expr, int dest);
    void compile(gc<Pattern> pattern, int slot);

    virtual void visit(AndExpr& expr, int dest);
    virtual void visit(AssignExpr& expr, int dest);
    virtual void visit(BinaryOpExpr& expr, int dest);
    virtual void visit(BoolExpr& expr, int dest);
    virtual void visit(CallExpr& expr, int dest);
    virtual void visit(CatchExpr& expr, int dest);
    virtual void visit(DefExpr& expr, int dest);
    virtual void visit(DoExpr& expr, int dest);
    virtual void visit(IfExpr& expr, int dest);
    virtual void visit(IsExpr& expr, int dest);
    virtual void visit(MatchExpr& expr, int dest);
    virtual void visit(NameExpr& expr, int dest);
    virtual void visit(NativeExpr& expr, int dest);
    virtual void visit(NotExpr& expr, int dest);
    virtual void visit(NothingExpr& expr, int dest);
    virtual void visit(NumberExpr& expr, int dest);
    virtual void visit(OrExpr& expr, int dest);
    virtual void visit(RecordExpr& expr, int dest);
    virtual void visit(ReturnExpr& expr, int dest);
    virtual void visit(SequenceExpr& expr, int dest);
    virtual void visit(StringExpr& expr, int dest);
    virtual void visit(ThrowExpr& expr, int dest);
    virtual void visit(VariableExpr& expr, int dest);
    virtual void visit(WhileExpr& expr, int dest);

    void compileMatch(const Array<MatchClause>& clauses, int dest);
    
    // Compiles the given expr. If it's a constant expr, it adds the constant
    // to the method table and returns the constant id (with the mask bit set).
    // Otherwise, creates a temporary slot and compiles the expr to evaluate
    // into that. It then returns the shot. The caller is required to release
    // release that slot when done with it.
    //
    // Some instructions like OP_END and OP_ADD can read an operand from a
    // slot or a constant. This is used to compile the operands for those.
    int compileExpressionOrConstant(gc<Expr> expr);
    
    int compileConstant(const NumberExpr& expr);
    int compileConstant(const StringExpr& expr);

    void write(OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    int startJump();
    int startJumpBack();
    
    // Backpatches the bytecode at `from` with the given instruction and the
    // given operands with an additional operand that is the offset from `from`
    // to the current instruction position.
    void endJump(int from, OpCode op, int a = -1, int b = -1);
    
    // Inserts a backwards jump to the given instruction.
    void endJumpBack(int to);
    
    int getNextTemp() const;
    int makeTemp();
    void releaseTemp();
    void releaseTemps(int count);
    
    Compiler& compiler_;

    // The module containing the method being compiled.
    Module* module_;
    
    // The chunk being compiled.
    gc<Chunk> chunk_;
    
    Array<instruction> code_;
    
    // The number of slots currently in use.
    int numLocals_;
    int numTemps_;
    int maxSlots_;
    
    NO_COPY(MethodCompiler);
  };
  
  // Locates a code offset and a slot where a pattern-match test operation
  // occurs. By default, the test instruction will be OP_TEST_MATCH, which
  // throws on failure. But for cases in a match expression, it should jump to
  // the next case if a match fails. This tracks these locations so we can
  // replace them with appropriate jumps.
  struct MatchTest
  {
    // Default constructor so we can use this in Array.
    MatchTest()
    : position(-1),
      slot(-1)
    {}
    
    MatchTest(int position, int slot)
    : position(position),
      slot(slot)
    {}
    
    // Location in the bytecode where the test instruction appears.
    int position;
    
    // Slot where the value of the test is stored.
    int slot;
  };
  
  class PatternCompiler : public PatternVisitor
  {
  public:
    PatternCompiler(MethodCompiler& compiler, bool jumpOnFailure = false)
    : compiler_(compiler),
      jumpOnFailure_(jumpOnFailure)
    {}
    
    void endJumps();
    
    virtual void visit(RecordPattern& pattern, int value);
    virtual void visit(TypePattern& pattern, int value);
    virtual void visit(ValuePattern& pattern, int value);
    virtual void visit(VariablePattern& pattern, int value);
    virtual void visit(WildcardPattern& pattern, int value);
  
  private:
    void writeTest(int slot);
    
    MethodCompiler& compiler_;
    bool jumpOnFailure_;
    Array<MatchTest> tests_;
  };
}