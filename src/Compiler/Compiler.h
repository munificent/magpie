#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Memory.h"
#include "Ast.h"

namespace magpie
{
  class ErrorReporter;
  class Method;
  class Module;
  class Expr;
  class Object;
  class PatternCompiler;
  class Scope;
  class VM;
  
  class Compiler : private ExprVisitor
  {
    friend class PatternCompiler;
    
  public:
    static Module* compileModule(VM& vm, gc<ModuleAst> module,
                                 ErrorReporter& reporter);
    static gc<Method> compileMethod(VM& vm, Module* module,
                                    MethodDef& method,
                                    ErrorReporter& reporter);
    
    virtual ~Compiler() {}
    
  private:    
    Compiler(VM& vm, ErrorReporter& reporter, Module* module);
    
    gc<Method> compile(MethodDef& method);

    void compileParam(gc<Pattern> param, int& slot);
    void compileParamField(gc<Pattern> param, int slot);
    int compileArg(gc<Expr> arg);
    
    void compile(gc<Expr> expr, int dest);
    void compile(gc<Pattern> pattern, int slot);

    virtual void visit(AndExpr& expr, int dest);
    virtual void visit(BinaryOpExpr& expr, int dest);
    virtual void visit(BoolExpr& expr, int dest);
    virtual void visit(CallExpr& expr, int dest);
    virtual void visit(CatchExpr& expr, int dest);
    virtual void visit(DoExpr& expr, int dest);
    virtual void visit(IfExpr& expr, int dest);
    virtual void visit(IsExpr& expr, int dest);
    virtual void visit(LoopExpr& expr, int dest);
    virtual void visit(MatchExpr& expr, int dest);
    virtual void visit(NameExpr& expr, int dest);
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
    
    // Backpatches the bytecode at `from` with the given instruction and the
    // given operands with an additional operand that is the offset from `from`
    // to the current instruction position.
    void endJump(int from, OpCode op, int a = -1, int b = -1);
    
    int getNextTemp() const;
    int makeTemp();
    void releaseTemp();
    void releaseTemps(int count);
    
    VM& vm_;
    ErrorReporter& reporter_;
    
    // The method being compiled.
    gc<Method> method_;
    
    Array<instruction> code_;
    
    // The number of slots currently in use.
    int numLocals_;
    int numTemps_;
    int maxSlots_;
    
    NO_COPY(Compiler);
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
    PatternCompiler(Compiler& compiler, bool jumpOnFailure = false)
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
    
    Compiler& compiler_;
    bool jumpOnFailure_;
    Array<MatchTest> tests_;
  };
  
  // Method definitions and calls are statically distinguished by the records
  // used for the left and right arguments (or parameters). For example, a
  // call to "foo(1, b: 2)" is statically known to be different from a call to
  // "foo(1)" or "1 foo".
  //
  // This class supports that by generating a "signature" string for a method
  // definition or call that contains both the method's name, and the structure
  // of its arguments. The above examples would have signatures "foo(,b)",
  // "foo()", and "()foo" respectively.
  class SignatureBuilder
  {
  public:
    // Builds a signature for the method being called by the given expr.
    static gc<String> build(const CallExpr& expr);
    
    // Builds a signature for the given method definition.
    static gc<String> build(const MethodDef& expr);
    
  private:
    SignatureBuilder()
    : length_(0)
    {}
    
    static const int MAX_LENGTH = 256; // TODO(bob): Use something dynamic.
    
    void writeArg(gc<Expr> expr);
    void writeParam(gc<Pattern> pattern);
    void add(gc<String> text);
    void add(const char* text);
    
    int length_;
    char signature_[MAX_LENGTH];
  };
}