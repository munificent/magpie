#pragma once

#include "Macros.h"
#include "Lexer.h"
#include "Queue.h"

namespace magpie
{
  class BinaryOpNode;
  class BoolNode;
  class IfNode;
  class Lexer;
  class Node;
  class NumberNode;
  class VariableNode;
  class VariablePattern;
  
  // Visitor pattern for dispatching on AST nodes. Implemented by the compiler.
  class NodeVisitor
  {
  public:
    virtual ~NodeVisitor() {}
    
    virtual void visit(const BoolNode& node, int dest) = 0;
    virtual void visit(const BinaryOpNode& node, int dest) = 0;
    virtual void visit(const IfNode& node, int dest) = 0;
    virtual void visit(const NumberNode& node, int dest) = 0;
    virtual void visit(const VariableNode& node, int dest) = 0;
    
  protected:
    NodeVisitor() {}
    
  private:
    NO_COPY(NodeVisitor);
  };
  
  // Visitor pattern for dispatching on AST pattern nodes. Implemented by the
  // compiler.
  class PatternVisitor
  {
  public:
    virtual ~PatternVisitor() {}
    
    virtual void visit(const VariablePattern& pattern, int dest) = 0;
    
  protected:
    PatternVisitor() {}
    
  private:
    NO_COPY(PatternVisitor);
  };
}
