#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Memory.h"
#include "NodeVisitor.h"

namespace magpie
{
  class Method;
  class Object;
  class VM;
  
  class Compiler : private NodeVisitor, private PatternVisitor
  {
  public:
    static void compileProgram(VM& vm, const Node& node);
    static temp<Method> compileMethod(const Node& node);
    
    virtual ~Compiler() {}
    
  private:
    Compiler();
    
    temp<Method> compile(const Node& node);
    
    virtual void visit(const BoolNode& node, int dest);
    virtual void visit(const BinaryOpNode& node, int dest);
    virtual void visit(const IfNode& node, int dest);
    virtual void visit(const MethodNode& node, int dest);
    virtual void visit(const NameNode& node, int dest);
    virtual void visit(const NumberNode& node, int dest);
    virtual void visit(const SequenceNode& node, int dest);
    virtual void visit(const VariableNode& node, int dest);

    virtual void visit(const VariablePattern& pattern, int value);

    void compileInfix(const BinaryOpNode& node, OpCode op, int dest);

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

    void write(OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    int startJump();
    void endJump(int from, OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    
    int allocateRegister();
    void releaseRegister();
    void reserveVariables(int count);
    int allocateVariable();

    Array<gc<String> > locals_;
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    int                numInUseRegisters_;
    int                variableStart_;
    int                maxRegisters_;

    NO_COPY(Compiler);
  };

}