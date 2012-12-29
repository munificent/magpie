#pragma once

#include "Array.h"
#include "Ast.h"
#include "Bytecode.h"
#include "Compiler.h"
#include "Memory.h"

namespace magpie
{
  class Loop;
  class Module;
  class PatternCompiler;

  class ExprCompiler : private ExprVisitor, private LValueVisitor
  {
    friend class Compiler;
    friend class Loop;
    friend class PatternCompiler;

  public:
    virtual ~ExprCompiler() {}

  private:
    ExprCompiler(Compiler& compiler);
    
    gc<Chunk> compileBody(Module* module, gc<Expr> body);

    // Compiles [multimethod] to bytecode. Assumes the methods have already
    // been sorted.
    gc<Chunk> compile(Multimethod& multimethod);

    // Compiles [function] to bytecode.
    gc<Chunk> compile(Module* module, FnExpr& function);

    // Compiles [expr] to bytecode.
    gc<Chunk> compile(Module* module, AsyncExpr& expr);

    void compile(Module* module, int maxLocals,
                 gc<Pattern> leftParam, gc<Pattern> rightParam,
                 gc<Pattern> valueParam, gc<Expr> body);

    void compileParam(PatternCompiler& compiler, gc<Pattern> param, int& slot);
    void compileParamField(PatternCompiler& compiler, gc<Pattern> param,
                           int slot);
    int compileArg(gc<Expr> arg);

    void compile(gc<Expr> expr, int dest);
    void compile(gc<Pattern> pattern, int slot);

    virtual void visit(AndExpr& expr, int dest);
    virtual void visit(AssignExpr& expr, int dest);
    virtual void visit(AsyncExpr& expr, int dest);
    virtual void visit(BinaryOpExpr& expr, int dest);
    virtual void visit(BoolExpr& expr, int dest);
    virtual void visit(BreakExpr& expr, int dest);
    virtual void visit(CallExpr& expr, int dest);
    virtual void visit(CatchExpr& expr, int dest);
    virtual void visit(CharacterExpr& expr, int dest);
    virtual void visit(DefExpr& expr, int dest);
    virtual void visit(DefClassExpr& expr, int dest);
    virtual void visit(DoExpr& expr, int dest);
    virtual void visit(FloatExpr& expr, int dest);
    virtual void visit(FnExpr& expr, int dest);
    virtual void visit(ForExpr& expr, int dest);
    virtual void visit(GetFieldExpr& expr, int dest);
    virtual void visit(IfExpr& expr, int dest);
    virtual void visit(ImportExpr& expr, int dest);
    virtual void visit(IntExpr& expr, int dest);
    virtual void visit(IsExpr& expr, int dest);
    virtual void visit(ListExpr& expr, int dest);
    virtual void visit(MatchExpr& expr, int dest);
    virtual void visit(NameExpr& expr, int dest);
    virtual void visit(NativeExpr& expr, int dest);
    virtual void visit(NotExpr& expr, int dest);
    virtual void visit(NothingExpr& expr, int dest);
    virtual void visit(OrExpr& expr, int dest);
    virtual void visit(RecordExpr& expr, int dest);
    virtual void visit(ReturnExpr& expr, int dest);
    virtual void visit(SetFieldExpr& expr, int dest);
    virtual void visit(SequenceExpr& expr, int dest);
    virtual void visit(StringExpr& expr, int dest);
    virtual void visit(ThrowExpr& expr, int dest);
    virtual void visit(VariableExpr& expr, int dest);
    virtual void visit(WhileExpr& expr, int dest);

    virtual void visit(CallLValue& lvalue, int value);
    virtual void visit(NameLValue& lvalue, int value);
    virtual void visit(RecordLValue& lvalue, int value);
    virtual void visit(WildcardLValue& lvalue, int value);

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

    int compileConstant(const CharacterExpr& expr);
    int compileConstant(const FloatExpr& expr);
    int compileConstant(const IntExpr& expr);
    int compileConstant(const StringExpr& expr);

    // Compiles a method call. If `valueSlot` is -1, then it's a regular call.
    // Otherwise, it's a call to a setter, and `valueSlot` is the slot holding
    // the right-hand side value.
    void compileCall(const CallExpr& expr, int dest, int valueSlot);
    void compileAssignment(gc<SourcePos> pos, gc<ResolvedName> resolved,
                           int value, bool isCreate);
    void compileClosures(gc<SourcePos> pos,
                         ResolvedProcedure& procedure);

    void write(const Expr& expr, OpCode op,
               int a = 0xff, int b = 0xff, int c = 0xff);
    void write(int line, OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    int startJump(const Expr& expr);
    int startJump(gc<SourcePos> pos);
    int startJumpBack();

    // Backpatches the bytecode at `from` with the given instruction and the
    // given operands with an additional operand that is the offset from `from`
    // to the current instruction position.
    void endJump(int from, OpCode op, int a = -1, int b = -1);

    // Inserts a backwards jump to the given instruction.
    void endJumpBack(const Expr& expr, int to);

    int getNextTemp() const;
    int makeTemp();
    void releaseTemp();
    void releaseTemps(int count);

    Compiler& compiler_;

    // The module containing the code being compiled.
    Module* module_;

    // The chunk being compiled.
    gc<Chunk> chunk_;

    // The number of slots currently in use.
    int numLocals_;
    int numTemps_;
    int maxSlots_;

    // The innermost loop currently being compiled.
    Loop* currentLoop_;

    // The index in the chunk's file list of the file containing the source
    // code currently being compiled.
    int currentFile_;

    NO_COPY(ExprCompiler);
  };

  // Locates a code offset and a slot where a pattern-match test operation
  // occurs. By default, the test instruction will be OP_TEST_MATCH, which
  // throws on failure. But for cases in a match expression, it should jump to
  // the next case if a match fails. This tracks these locations so we can
  // replace them with appropriate jumps.
  struct MatchTest
  {
    MatchTest(int position, int slot)
    : position(position),
      slot(slot)
    {}

    // Location in the bytecode where the test instruction appears.
    int position;

    // Slot where the value of the test is stored.
    int slot;
  };

  // Keeps track of the break expressions that have appeared in the current
  // loop. When the loop body ends, these will be compiled to appropriate jumps.
  class Loop
  {
  public:
    Loop(ExprCompiler* compiler);
    ~Loop();

    void addBreak(const Expr& expr);
    void end();
    
  private:
    ExprCompiler* compiler_;
    
    // The enclosing loop, or NULL if this is the outermost loop.
    Loop* parent_;

    // The offsets of each break's jump.
    Array<int> breaks_;

    // TODO(bob): Make a macro that ensures that this can only be allocated on
    // the stack.
  };

  class PatternCompiler : public PatternVisitor
  {
  public:
    PatternCompiler(ExprCompiler& compiler, bool jumpOnFailure = false)
    : compiler_(compiler),
      jumpOnFailure_(jumpOnFailure)
    {}

    void compile(gc<Pattern> pattern, int slot);

    void endJumps();

    virtual void visit(RecordPattern& pattern, int slot);
    virtual void visit(TypePattern& pattern, int slot);
    virtual void visit(ValuePattern& pattern, int slot);
    virtual void visit(VariablePattern& pattern, int slot);
    virtual void visit(WildcardPattern& pattern, int slot);

  private:
    void writeTest(const Pattern& pattern, int slot);

    ExprCompiler& compiler_;
    bool jumpOnFailure_;
    Array<MatchTest> tests_;
  };
}
