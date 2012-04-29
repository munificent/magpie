#include "Node.h"

namespace magpie
{
  ModuleAst::ModuleAst(Array<gc<MethodAst> >& methods)
  : methods_(methods)
  {}
  
  void ModuleAst::reach()
  {
    Memory::reach(methods_);
  }
  
  MethodAst::MethodAst(gc<String> name, gc<Pattern> parameter, gc<Node> body)
  : name_(name),
    parameter_(parameter),
    body_(body)
  {}
  
  void MethodAst::reach()
  {
    Memory::reach(name_);
    Memory::reach(parameter_);
    Memory::reach(body_);
  }
  
  BinaryOpNode::BinaryOpNode(const SourcePos& pos,
                             gc<Node> left, TokenType type, gc<Node> right)
  : Node(pos),
    left_(left),
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
  
  BoolNode::BoolNode(const SourcePos& pos, bool value)
  : Node(pos),
    value_(value)
  {}
  
  void BoolNode::trace(std::ostream& out) const
  {
    out << (value_ ? "true" : "false");
  }
  
  CallNode::CallNode(const SourcePos& pos,
                     gc<Node> leftArg, gc<String> name, gc<Node> rightArg)
  : Node(pos),
    leftArg_(leftArg),
    name_(name),
    rightArg_(rightArg)
  {}
  
  void CallNode::reach()
  {
    Memory::reach(leftArg_);
    Memory::reach(name_);
    Memory::reach(rightArg_);
  }
  
  void CallNode::trace(std::ostream& out) const
  {
    out << leftArg_ << " " << name_ << "(" << rightArg_ << ")";
  }
  
  DefMethodNode::DefMethodNode(const SourcePos& pos, gc<String> name,
                               gc<Pattern> parameter, gc<Node> body)
  : Node(pos),
    name_(name),
    parameter_(parameter),
    body_(body)
  {}
  
  void DefMethodNode::reach()
  {
    Memory::reach(name_);
    Memory::reach(parameter_);
    Memory::reach(body_);
  }
  
  IfNode::IfNode(const SourcePos& pos, gc<Node> condition,
                 gc<Node> thenArm, gc<Node> elseArm)
  : Node(pos),
    condition_(condition),
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
    
  NameNode::NameNode(const SourcePos& pos, gc<String> name)
  : Node(pos),
    name_(name)
  {}
  
  void NameNode::reach()
  {
    Memory::reach(name_);
  }
  
  void NameNode::trace(std::ostream& out) const
  {
    out << name_;
  }
  
  NumberNode::NumberNode(const SourcePos& pos, double value)
  : Node(pos),
    value_(value)
  {}
  
  void NumberNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  SequenceNode::SequenceNode(const SourcePos& pos,
                             const Array<gc<Node> >& expressions)
  : Node(pos),
    expressions_(expressions)
  {}
  
  void SequenceNode::reach()
  {
    Memory::reach(expressions_);
  }

  void SequenceNode::trace(std::ostream& out) const
  {
    for (int i = 0; i < expressions_.count(); i++)
    {
      out << expressions_[i] << "\n";
    }
  }
  
  StringNode::StringNode(const SourcePos& pos, gc<String> value)
  : Node(pos),
    value_(value)
  {}
  
  void StringNode::reach()
  {
    Memory::reach(value_);
  }
  
  void StringNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  VariableNode::VariableNode(const SourcePos& pos, bool isMutable,
                             gc<Pattern> pattern, gc<Node> value)
  : Node(pos),
    isMutable_(isMutable),
    pattern_(pattern),
    value_(value)
  {}
  
  void VariableNode::reach()
  {
    Memory::reach(pattern_);
    Memory::reach(value_);
  }
  
  void VariableNode::trace(std::ostream& out) const
  {
    out << (isMutable_ ? "var " : "val ");
    out << pattern_ << " = " << value_;
  }
  
  VariablePattern::VariablePattern(gc<String> name)
  : name_(name)
  {}
  
  void VariablePattern::reach()
  {
    Memory::reach(name_);
  }
  
  void VariablePattern::trace(std::ostream& out) const
  {
    out << name_;
  }
}

