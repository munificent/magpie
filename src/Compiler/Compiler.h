#pragma once

#include "Array.h"
#include "Bytecode.h"
#include "GC.h"
#include "NodeVisitor.h"

namespace magpie
{
  class Compiler : private NodeVisitor
  {
  public:
    Compiler()
    : NodeVisitor()
    {}
    
    virtual ~Compiler() {}
    
    virtual void visit(BinaryOpNode& node, int dest);
    virtual void visit(NumberNode& node, int dest);

  private:
    void compileConstant(temp<Managed> value, int dest);
    
    void write(OpCode op, int a = 0xff, int b = 0xff, int c = 0xff);
    
    Array<gc<Managed> > constants_;
    Array<instruction>  code_;
    
    NO_COPY(Compiler);
  };

}