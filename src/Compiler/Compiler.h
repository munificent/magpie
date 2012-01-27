#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "Memory.h"
#include "NodeVisitor.h"

namespace magpie
{
  class Method;
  class Object;
  
  class Compiler : private NodeVisitor
  {
  public:
    static temp<Method> compileMethod(const Node& node);
    
    virtual ~Compiler() {}
    
  private:
    Compiler(temp<Method> method);
    
    void compile(const Node& node);
    
    virtual void visit(const BinaryOpNode& node, int dest);
    virtual void visit(const NumberNode& node, int dest);
    
    void compileConstant(temp<Object> value, int dest);
    
    void write(OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    
    int reserveRegister();
    void releaseRegister();

    // The method being compiled.
    gc<Method> method_;
    int        numInUseRegisters_;

    NO_COPY(Compiler);
  };

}