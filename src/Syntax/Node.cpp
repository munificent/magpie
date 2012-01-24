#include "Node.h"

namespace magpie
{
  temp<BinaryOpNode> BinaryOpNode::create(gc<Node> left, TokenType type,
                                          gc<Node> right)
  {
    return Memory::makeTemp(new BinaryOpNode(left, type, right));
  }
  
  BinaryOpNode::BinaryOpNode(gc<Node> left, TokenType type, gc<Node> right)
  : left_(left),
    type_(type),
    right_(right)
  {}

  void BinaryOpNode::reach()
  {
    Memory::reach(left_);
    Memory::reach(right_);
  }

  void BinaryOpNode::trace(std::ostream& out) const
  {
    out << "(" << left_ << " " << Token::typeString(type_)
        << " " << right_ << ")";
  }
  
  temp<NumberNode> NumberNode::create(double value)
  {
    return Memory::makeTemp(new NumberNode(value));
  }
  
  NumberNode::NumberNode(double value)
  : value_(value)
  {}
  
  void NumberNode::trace(std::ostream& out) const
  {
    out << value_;
  }
}

