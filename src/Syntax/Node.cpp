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
  
  temp<BoolNode> BoolNode::create(bool value)
  {
    return Memory::makeTemp(new BoolNode(value));
  }
  
  BoolNode::BoolNode(bool value)
  : value_(value)
  {}
  
  void BoolNode::trace(std::ostream& out) const
  {
    out << (value_ ? "true" : "false");
  }
  
  temp<IfNode> IfNode::create(gc<Node> condition, gc<Node> thenArm,
                              gc<Node> elseArm)
  {
    return Memory::makeTemp(new IfNode(condition, thenArm, elseArm));
  }
  
  IfNode::IfNode(gc<Node> condition, gc<Node> thenArm, gc<Node> elseArm)
  : condition_(condition),
    thenArm_(thenArm),
    elseArm_(elseArm)
  {}
  
  void IfNode::reach()
  {
    Memory::reach(condition_);
    Memory::reach(thenArm_);
    Memory::reach(elseArm_);
  }
  
  void IfNode::trace(std::ostream& out) const
  {
    out << "(if " << condition_ << " then " << thenArm_;
    
    if (elseArm_.isNull())
    {
      out << ")";
    }
    else
    {
      out << " else " << elseArm_ << ")";
    }
  }
  
  temp<MethodNode> MethodNode::create(gc<String> name, gc<Node> body)
  {
    return Memory::makeTemp(new MethodNode(name, body));
  }
  
  MethodNode::MethodNode(gc<String> name, gc<Node> body)
  : name_(name),
    body_(body)
  {}
  
  void MethodNode::reach()
  {
    Memory::reach(name_);
    Memory::reach(body_);
  }
  
  void MethodNode::trace(std::ostream& out) const
  {
    out << "def " << name_ << "()" << body_;
  }
  
  temp<NameNode> NameNode::create(gc<String> name)
  {
    return Memory::makeTemp(new NameNode(name));
  }
  
  NameNode::NameNode(gc<String> name)
  : name_(name)
  {}
  
  void NameNode::reach()
  {
    Memory::reach(name_);
  }
  
  void NameNode::trace(std::ostream& out) const
  {
    out << name_;
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
  
  temp<SequenceNode> SequenceNode::create(const Array<gc<Node> >& expressions)
  {
    return Memory::makeTemp(new SequenceNode(expressions));
  }
  
  SequenceNode::SequenceNode(const Array<gc<Node> >& expressions)
  : expressions_(expressions)
  {}
  
  void SequenceNode::trace(std::ostream& out) const
  {
    for (int i = 0; i < expressions_.count(); i++)
    {
      out << expressions_[i] << "\n";
    }
  }
  
  temp<VariableNode> VariableNode::create(bool isMutable, gc<Pattern> pattern,
                                          gc<Node> value)
  {
    return Memory::makeTemp(new VariableNode(isMutable, pattern, value));
  }
  
  VariableNode::VariableNode(bool isMutable, gc<Pattern> pattern,
                             gc<Node> value)
  : isMutable_(isMutable),
    pattern_(pattern),
    value_(value)
  {}
  
  void VariableNode::trace(std::ostream& out) const
  {
    out << (isMutable_ ? "var " : "val ");
    out << pattern_ << " = " << value_;
  }
  
  temp<VariablePattern> VariablePattern::create(gc<String> name)
  {
    return Memory::makeTemp(new VariablePattern(name));
  }
  
  VariablePattern::VariablePattern(gc<String> name)
  : name_(name)
  {}
  
  void VariablePattern::trace(std::ostream& out) const
  {
    out << name_;
  }
}

