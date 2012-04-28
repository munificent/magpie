#include "Node.h"

namespace magpie
{
  temp<ModuleAst> ModuleAst::create(Array<gc<MethodAst> >& methods)
  {
    return Memory::makeTemp(new ModuleAst(methods));
  }
  
  ModuleAst::ModuleAst(Array<gc<MethodAst> >& methods)
  : methods_(methods)
  {}
  
  void ModuleAst::reach()
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      Memory::reach(methods_[i]);
    }
  }
  
  temp<MethodAst> MethodAst::create(gc<String> name, gc<Pattern> parameter,
                                    gc<Node> body)
  {
    return Memory::makeTemp(new MethodAst(name, parameter, body));
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
  
  void MethodAst::trace(std::ostream& out) const
  {
    out << "def " << name_ << "()" << body_;
  }
  
  temp<BinaryOpNode> BinaryOpNode::create(const SourcePos& pos,
                                          gc<Node> left, TokenType type,
                                          gc<Node> right)
  {
    return Memory::makeTemp(new BinaryOpNode(pos, left, type, right));
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
  
  temp<BoolNode> BoolNode::create(const SourcePos& pos, bool value)
  {
    return Memory::makeTemp(new BoolNode(pos, value));
  }
  
  BoolNode::BoolNode(const SourcePos& pos, bool value)
  : Node(pos),
    value_(value)
  {}
  
  void BoolNode::trace(std::ostream& out) const
  {
    out << (value_ ? "true" : "false");
  }
  
  temp<CallNode> CallNode::create(const SourcePos& pos,
      gc<Node> leftArg, gc<String> name, gc<Node> rightArg)
  {
    return Memory::makeTemp(new CallNode(pos, leftArg, name, rightArg));
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
  
  temp<IfNode> IfNode::create(const SourcePos& pos, gc<Node> condition,
                              gc<Node> thenArm, gc<Node> elseArm)
  {
    return Memory::makeTemp(new IfNode(pos, condition, thenArm, elseArm));
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
    
  temp<NameNode> NameNode::create(const SourcePos& pos, gc<String> name)
  {
    return Memory::makeTemp(new NameNode(pos, name));
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
  
  temp<NumberNode> NumberNode::create(const SourcePos& pos, double value)
  {
    return Memory::makeTemp(new NumberNode(pos, value));
  }
  
  NumberNode::NumberNode(const SourcePos& pos, double value)
  : Node(pos),
    value_(value)
  {}
  
  void NumberNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  temp<SequenceNode> SequenceNode::create(const SourcePos& pos,
                                          const Array<gc<Node> >& expressions)
  {
    return Memory::makeTemp(new SequenceNode(pos, expressions));
  }
  
  SequenceNode::SequenceNode(const SourcePos& pos,
                             const Array<gc<Node> >& expressions)
  : Node(pos),
    expressions_(expressions)
  {}
  
  void SequenceNode::trace(std::ostream& out) const
  {
    for (int i = 0; i < expressions_.count(); i++)
    {
      out << expressions_[i] << "\n";
    }
  }
  
  temp<StringNode> StringNode::create(const SourcePos& pos, gc<String> value)
  {
    return Memory::makeTemp(new StringNode(pos, value));
  }
  
  StringNode::StringNode(const SourcePos& pos, gc<String> value)
  : Node(pos),
    value_(value)
  {}
  
  void StringNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  temp<VariableNode> VariableNode::create(const SourcePos& pos, bool isMutable,
                                          gc<Pattern> pattern, gc<Node> value)
  {
    return Memory::makeTemp(new VariableNode(pos, isMutable, pattern, value));
  }
  
  VariableNode::VariableNode(const SourcePos& pos, bool isMutable,
                             gc<Pattern> pattern, gc<Node> value)
  : Node(pos),
    isMutable_(isMutable),
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

