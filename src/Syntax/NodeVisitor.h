#pragma once

#include "Macros.h"
#include "Lexer.h"
#include "Queue.h"

namespace magpie
{
  class BinaryOpNode;
  class IfNode;
  class Lexer;
  class Node;
  class NumberNode;
  
  // Visitor pattern for dispatching on AST nodes. Implemented by the compiler.
  class NodeVisitor
  {
  public:
    virtual ~NodeVisitor() {}
    
    virtual void visit(const BinaryOpNode& node, int dest) = 0;
    virtual void visit(const IfNode& node, int dest) = 0;
    virtual void visit(const NumberNode& node, int dest) = 0;
    
  protected:
    NodeVisitor() {}
    
  private:
    NO_COPY(NodeVisitor);
  };
}
