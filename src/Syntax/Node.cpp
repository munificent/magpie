#include "Node.h"

namespace magpie
{
  void AndNode::trace(std::ostream& out) const
  {
    out << "(and " << left_ << " " << right_ << ")";
  }
  
  void BinaryOpNode::trace(std::ostream& out) const
  {
    out << "(" << left_ << " " << Token::typeString(type_)
        << " " << right_ << ")";
  }

  void BoolNode::trace(std::ostream& out) const
  {
    out << (value_ ? "true" : "false");
  }
  
  void CallNode::trace(std::ostream& out) const
  {
    out << leftArg_ << " " << name_ << "(" << rightArg_ << ")";
  }
  
  void DefMethodNode::trace(std::ostream& out) const
  {
    // TODO(bob): Implement.
    out << "(def)";
  }
  
  void DoNode::trace(std::ostream& out) const
  {
    out << "(do " << body_ << ")";
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

  void NameNode::trace(std::ostream& out) const
  {
    out << name_;
  }
  
  void NothingNode::trace(std::ostream& out) const
  {
    out << "nothing";
  }
  
  void NumberNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  void OrNode::trace(std::ostream& out) const
  {
    out << "(or " << left_ << " " << right_ << ")";
  }
  
  void ReturnNode::trace(std::ostream& out) const
  {
    if (value_.isNull())
    {
      out << "(return)";
    }
    else
    {
      out << "(return " << value_ << ")";
    }
  }
  
  void SequenceNode::trace(std::ostream& out) const
  {
    for (int i = 0; i < expressions_.count(); i++)
    {
      out << expressions_[i] << "\n";
    }
  }
  
  void StringNode::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  void VariableNode::trace(std::ostream& out) const
  {
    out << (isMutable_ ? "var " : "val ");
    out << pattern_ << " = " << value_;
  }
  
  void NothingPattern::trace(std::ostream& out) const
  {
    out << "nothing";
  }
  
  void VariablePattern::trace(std::ostream& out) const
  {
    out << name_;
  }
}

