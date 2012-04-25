#pragma once

#include "Macros.h"
#include "Lexer.h"
#include "Queue.h"

namespace magpie
{
  class BinaryOpNode;
  class BoolNode;
  class CallNode;
  class IfNode;
  class Lexer;
  class Node;
  class NameNode;
  class NumberNode;
  class SequenceNode;
  class StringNode;
  class VariableNode;
  class VariablePattern;
  
  // Visitor pattern for dispatching on AST nodes. Implemented by the compiler.
  class NodeVisitor
  {
  public:
    virtual ~NodeVisitor() {}
    
    virtual void visit(const BinaryOpNode& node, int dest) = 0;
    virtual void visit(const BoolNode& node, int dest) = 0;
    virtual void visit(const CallNode& node, int dest) = 0;
    virtual void visit(const IfNode& node, int dest) = 0;
    virtual void visit(const NameNode& node, int dest) = 0;
    virtual void visit(const NumberNode& node, int dest) = 0;
    virtual void visit(const SequenceNode& node, int dest) = 0;
    virtual void visit(const StringNode& node, int dest) = 0;
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
