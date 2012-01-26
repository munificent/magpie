#pragma once

#include "Macros.h"
#include "Managed.h"
#include "NodeVisitor.h"
#include "Token.h"

#define DECLARE_NODE                                      \
virtual void accept(NodeVisitor& visitor, int arg) const  \
    {                                                     \
      visitor.visit(*this, arg);                          \
    }

namespace magpie
{
  using std::ostream;
  
  // Base class for all AST node classes.
  class Node : public Managed
  {
  public:
    virtual ~Node() {}

    // The visitor pattern.
    virtual void accept(NodeVisitor& visitor, int arg) const = 0;
  };
  
  // A binary operator.
  class BinaryOpNode : public Node
  {
  public:
    static temp<BinaryOpNode> create(gc<Node> left, TokenType type,
                                     gc<Node> right);
    
    DECLARE_NODE;
    
    Node& left() const { return *left_; }
    TokenType type() const { return type_; }
    Node& right() const { return *right_; }
    
    virtual void reach();
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
  
    DECLARE_NODE;
    
    double value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;

  private:
    NumberNode(double value);
    
    double value_;
  };
}

