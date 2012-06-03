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
  class VM;
  
  class Compiler : private ExprVisitor
  {
    friend class PatternCompiler;
    
  public:
    static Module* compileModule(VM& vm, gc<ModuleAst> module,
                                 ErrorReporter& reporter);
    static gc<Method> compileMethod(VM& vm, Module* module,
                                    const MethodDef& method,
                                    ErrorReporter& reporter);
    
    virtual ~Compiler() {}
    
  private:
    // Keeps track of local variable scopes to handle nesting and shadowing.
    // This class can also visit patterns in order to create registers for the
    // variables declared in a pattern.
    class Scope : public PatternVisitor
    {
    public:
      Scope(Compiler* compiler);      
      ~Scope();
      
      int makeLocal(const SourcePos& pos, gc<String> name);
      void end();
      
      virtual void visit(const RecordPattern& pattern, int value);
      virtual void visit(const TypePattern& pattern, int value);
      virtual void visit(const ValuePattern& pattern, int value);
      virtual void visit(const VariablePattern& pattern, int value);
      virtual void visit(const WildcardPattern& pattern, int value);

    private:
      Compiler& compiler_;
      Scope* parent_;
      int start_;
      
      NO_COPY(Scope);
    };
    
    Compiler(VM& vm, ErrorReporter& reporter, Module* module);
    
    gc<Method> compile(const MethodDef& method);

    virtual void visit(const AndExpr& expr, int dest);
    virtual void visit(const BinaryOpExpr& expr, int dest);
    virtual void visit(const BoolExpr& expr, int dest);
    virtual void visit(const CallExpr& expr, int dest);
    virtual void visit(const CatchExpr& expr, int dest);
    virtual void visit(const DoExpr& expr, int dest);
    virtual void visit(const IfExpr& expr, int dest);
    virtual void visit(const IsExpr& expr, int dest);
    virtual void visit(const MatchExpr& expr, int dest);
    virtual void visit(const NameExpr& expr, int dest);
    virtual void visit(const NotExpr& expr, int dest);
    virtual void visit(const NothingExpr& expr, int dest);
    virtual void visit(const NumberExpr& expr, int dest);
    virtual void visit(const OrExpr& expr, int dest);
    virtual void visit(const RecordExpr& expr, int dest);
    virtual void visit(const ReturnExpr& expr, int dest);
    virtual void visit(const SequenceExpr& expr, int dest);
    virtual void visit(const StringExpr& expr, int dest);
    virtual void visit(const ThrowExpr& expr, int dest);
    virtual void visit(const VariableExpr& expr, int dest);

    void compilePattern(gc<Pattern> pattern, int dest);
    
    // Compiles the given expr. If it's a constant expr, it adds the constant
    // to the method table and returns the constant id (with the mask bit set).
    // Otherwise, creates a temporary register and compiles the expr to evaluate
    // into that. It then returns the register. The caller is required to
    // release that register when done with it.
    //
    // Some instructions like OP_END and OP_ADD can read an operatand from a
    // register or a constant. This is used to compile the operands for those.
    int compileExpressionOrConstant(const Expr& expr);
    
    int compileConstant(const NumberExpr& expr);
    int compileConstant(const StringExpr& expr);

    // Walks the pattern and allocates locals for any variable patterns
    // encountered. We do this in a separate step so we can tell how many locals
    // we need for the pattern since the value temporary will come after that.
    void reserveVariables(const Pattern& pattern);
    
    void write(OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    int startJump();
    
    // Backpatches the bytecode at `from` with the given instruction and the
    // given operands with an additional operand that is the offset from `from`
    // to the current instruction position.
    void endJump(int from, OpCode op, int a = -1, int b = -1);
    
    int makeTemp();
    void releaseTemp();
    void updateMaxRegisters();
    
    VM& vm_;
    ErrorReporter& reporter_;
    
    // The method being compiled.
    gc<Method> method_;
    
    // The names of the current in-scope local variables (including all outer
    // scopes). The indices in this array correspond to the registers where
    // those locals are stored.
    Array<gc<String> > locals_;
    
    Array<instruction> code_;
    
    // The number of temporary registers currently in use.
    int numTemps_;
    int maxRegisters_;
    
    // The current inner-most local variable scope.
    Scope* scope_;
    
    NO_COPY(Compiler);
  };
  
  // Locates a code offset and a register where a pattern-match test operation
  // occurs. By default, the test instruction will be OP_TEST_MATCH, which
  // throws on failure. But for cases in a match expression, it should jump to
  // the next case if a match fails. This tracks these locations so we can
  // replace them with appropriate jumps.
  struct matchTest
  {
    // Default constructor so we can use this in Array.
    matchTest()
    : position(-1),
      value(-1)
    {}
    
    matchTest(int position, int value)
    : position(position),
      value(value)
    {}
    
    // Location in the bytecode where the test instruction appears.
    int position;
    
    // Register where the value of the test is stored.
    int value;
  };
  
  class PatternCompiler : public PatternVisitor
  {
  public:
    PatternCompiler(Compiler& compiler, bool jumpOnFailure = false)
    : compiler_(compiler),
      jumpOnFailure_(jumpOnFailure)
    {}
    
    void endJumps();
    
    virtual void visit(const RecordPattern& pattern, int value);
    virtual void visit(const TypePattern& pattern, int value);
    virtual void visit(const ValuePattern& pattern, int value);
    virtual void visit(const VariablePattern& pattern, int value);
    virtual void visit(const WildcardPattern& pattern, int value);
  
  private:
    void writeTest(int reg);
    
    Compiler& compiler_;
    bool jumpOnFailure_;
    Array<matchTest> tests_;
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