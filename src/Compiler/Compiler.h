#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Memory.h"
#include "Node.h"

namespace magpie
{
  class ErrorReporter;
  class Method;
  class Module;
  class Node;
  class Object;
  class VM;
  
  class Compiler : private NodeVisitor, private PatternVisitor
  {
  public:
    static Module* compileModule(VM& vm, gc<Node> module,
                                 ErrorReporter& reporter);
    static gc<Method> compileMethod(VM& vm, Module* module,
                                    const DefMethodNode& method,
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
      
      virtual void visit(const NothingPattern& pattern, int value);
      virtual void visit(const RecordPattern& pattern, int value);
      virtual void visit(const TypePattern& pattern, int value);
      virtual void visit(const ValuePattern& pattern, int value);
      virtual void visit(const VariablePattern& pattern, int value);

    private:
      Compiler& compiler_;
      Scope* parent_;
      int start_;
      
      NO_COPY(Scope);
    };
    
    Compiler(VM& vm, ErrorReporter& reporter, Module* module);
    
    gc<Method> compile(const DefMethodNode& methodAst);
    
    virtual void visit(const AndNode& node, int dest);
    virtual void visit(const BinaryOpNode& node, int dest);
    virtual void visit(const BoolNode& node, int dest);
    virtual void visit(const CallNode& node, int dest);
    virtual void visit(const CatchNode& node, int dest);
    virtual void visit(const DefMethodNode& node, int dest);
    virtual void visit(const DoNode& node, int dest);
    virtual void visit(const IfNode& node, int dest);
    virtual void visit(const IsNode& node, int dest);
    virtual void visit(const NameNode& node, int dest);
    virtual void visit(const NotNode& node, int dest);
    virtual void visit(const NothingNode& node, int dest);
    virtual void visit(const NumberNode& node, int dest);
    virtual void visit(const OrNode& node, int dest);
    virtual void visit(const RecordNode& node, int dest);
    virtual void visit(const ReturnNode& node, int dest);
    virtual void visit(const SequenceNode& node, int dest);
    virtual void visit(const StringNode& node, int dest);
    virtual void visit(const ThrowNode& node, int dest);
    virtual void visit(const VariableNode& node, int dest);

    virtual void visit(const NothingPattern& pattern, int value);
    virtual void visit(const RecordPattern& pattern, int value);
    virtual void visit(const TypePattern& pattern, int value);
    virtual void visit(const ValuePattern& pattern, int value);
    virtual void visit(const VariablePattern& pattern, int value);

    // Compiles the given node. If it's a constant node, it adds the constant
    // to the method table and returns the constant id (with the mask bit set).
    // Otherwise, creates a temporary register and compiles the node to evaluate
    // into that. It then returns the register. The caller is required to
    // release that register when done with it.
    //
    // Some instructions like OP_END and OP_ADD can read an operatand from a
    // register or a constant. This is used to compile the operands for those.
    int compileExpressionOrConstant(const Node& node);
    
    int compileConstant(const NumberNode& node);
    int compileConstant(const StringNode& node);

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
    // Builds a signature for the method being called by the given node.
    static gc<String> build(const CallNode& node);
    
    // Builds a signature for the given method definition.
    static gc<String> build(const DefMethodNode& node);
    
  private:
    SignatureBuilder()
    : length_(0)
    {}
    
    static const int MAX_LENGTH = 256; // TODO(bob): Use something dynamic.
    
    void writeArg(gc<Node> node);
    void writeParam(gc<Pattern> pattern);
    void add(gc<String> text);
    void add(const char* text);
    
    int length_;
    char signature_[MAX_LENGTH];
  };
}