#pragma once

#include "Macros.h"
#include "Managed.h"
#include "Token.h"

/*
#define NODE_VISITOR                                             \
virtual void accept(IExprCompiler & compiler, int dest) const   \
{                                                               \
compiler.Visit(*this, dest);                                \
}
*/

namespace magpie
{
  using std::ostream;
  
  // Base class for all AST node classes.
  class Node : public Managed
  {
  public:
    virtual ~Node() {}
    /*
    // The visitor pattern.
    virtual void Accept(IExprCompiler & compiler, int dest) const = 0;
    
    virtual void Trace(std::ostream & stream) const = 0;
    */
  };
  
  // A binary operator.
  class BinaryOpNode : public Node
  {
  public:
    static temp<BinaryOpNode> create(gc<Node> left, TokenType type,
                                     gc<Node> right);
    
    Node& left() const { return *left_; }
    TokenType type() const { return type_; }
    Node& right() const { return *right_; }
    
    virtual void trace(std::ostream& out) const;

  private:
    BinaryOpNode(gc<Node> left, TokenType type, gc<Node> right);
    
    gc<Node>  left_;
    TokenType type_;
    gc<Node>  right_;
  };
  
  // A number literal.
  class NumberNode : public Node
  {
  public:
    static temp<NumberNode> create(double value);
  
    double value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;

  private:
    NumberNode(double value);
    
    double value_;
  };
}

