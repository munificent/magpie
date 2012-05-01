#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Memory.h"
#include "Node.h"

namespace magpie
{
  class ErrorReporter;
  class Method;
  class MethodAst;
  class ModuleAst;
  class Object;
  class VM;
  
  class Compiler : private NodeVisitor, private PatternVisitor
  {
  public:
    static void compileModule(VM& vm, gc<ModuleAst> module,
                              ErrorReporter& reporter);
    static gc<Method> compileMethod(VM& vm, gc<MethodAst> method,
                                    ErrorReporter& reporter);
    
    virtual ~Compiler() {}
    
  private:
    Compiler(VM& vm, ErrorReporter& reporter);
    
    gc<Method> compile(gc<MethodAst> method);
    
    virtual void visit(const AndNode& node, int dest);
    virtual void visit(const BinaryOpNode& node, int dest);
    virtual void visit(const BoolNode& node, int dest);
    virtual void visit(const CallNode& node, int dest);
    virtual void visit(const DefMethodNode& node, int dest);
    virtual void visit(const DoNode& node, int dest);
    virtual void visit(const IfNode& node, int dest);
    virtual void visit(const NameNode& node, int dest);
    virtual void visit(const NothingNode& node, int dest);
    virtual void visit(const NumberNode& node, int dest);
    virtual void visit(const OrNode& node, int dest);
    virtual void visit(const SequenceNode& node, int dest);
    virtual void visit(const StringNode& node, int dest);
    virtual void visit(const VariableNode& node, int dest);

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
    void declarePattern(const Pattern& pattern);

    void write(OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    int startJump();
    void endJump(int from, OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    
    int startScope();
    void endScope(int numLocals);
    int makeLocal(gc<String> name);
    int makeTemp();
    void releaseTemp();
    void updateMaxRegisters();
    
    VM&                vm_;
    ErrorReporter&     reporter_;
    Array<gc<String> > locals_;
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    int                numTemps_;
    int                maxRegisters_;
    
    NO_COPY(Compiler);
  };

}