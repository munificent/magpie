#include "Node.h"

namespace magpie
{
  ModuleAst::ModuleAst(Array<MethodAst*>& methods)
  : methods_(methods)
  {}
  
  ModuleAst::~ModuleAst()
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      delete methods_[i];
    }
  }
  
  MethodAst::MethodAst(gc<String> name, Pattern* parameter, Node* body)
  : name_(name),
    parameter_(parameter),
    body_(body)
  {}
  
  MethodAst::~MethodAst()
  {
    delete parameter_;
    delete body_;
    // TODO
    /*
    Memory::reach(name_);
     */
  }
  
  BinaryOpNode::BinaryOpNode(const SourcePos& pos,
                             Node* left, TokenType type, Node* right)
  : Node(pos),
    left_(left),
    type_(type),
    right_(right)
  {}

  BinaryOpNode::~BinaryOpNode()
  {
    delete left_;
    delete right_;
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
                     Node* leftArg, gc<String> name, Node* rightArg)
  : Node(pos),
    leftArg_(leftArg),
    name_(name),
    rightArg_(rightArg)
  {}
  
  CallNode::~CallNode()
  {
    delete leftArg_;
    //    Memory::reach(name_);
    delete rightArg_;
  }
  
  void CallNode::trace(std::ostream& out) const
  {
    out << leftArg_ << " " << name_ << "(" << rightArg_ << ")";
  }
  
  IfNode::IfNode(const SourcePos& pos, Node* condition,
                 Node* thenArm, Node* elseArm)
  : Node(pos),
    condition_(condition),
    thenArm_(thenArm),
    elseArm_(elseArm)
  {}
  
  IfNode::~IfNode()
  {
    delete condition_;
    delete thenArm_;
    delete elseArm_;
  }
  
  void IfNode::trace(std::ostream& out) const
  {
    out << "(if " << condition_ << " then " << thenArm_;
    
    if (elseArm_ == NULL)
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
  
  NameNode::~NameNode()
  {
    //    Memory::reach(name_);
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
                             const Array<Node*>& expressions)
  : Node(pos),
    expressions_(expressions)
  {}
  
  SequenceNode::~SequenceNode()
  {
    for (int i = 0; i < expressions_.count(); i++)
    {
      delete expressions_[i];
    }
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
  
  void StringNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  VariableNode::VariableNode(const SourcePos& pos, bool isMutable,
                             Pattern* pattern, Node* value)
  : Node(pos),
    isMutable_(isMutable),
    pattern_(pattern),
    value_(value)
  {}
  
  VariableNode::~VariableNode()
  {
    delete pattern_;
    delete value_;
  }
  
  void VariableNode::trace(std::ostream& out) const
  {
    out << (isMutable_ ? "var " : "val ");
    out << pattern_ << " = " << value_;
  }
  
  VariablePattern::VariablePattern(gc<String> name)
  : name_(name)
  {}
  
  void VariablePattern::trace(std::ostream& out) const
  {
    out << name_;
  }
}

