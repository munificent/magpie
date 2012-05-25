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
    out << "(";
    if (!leftArg_.isNull())
    {
      out << leftArg_ << " ";
    }
    
    out << name_;
    
    if (!rightArg_.isNull())
    {
      out << " " << rightArg_;
    }
    
    out << ")";
  }
  
  void CatchNode::trace(std::ostream& out) const
  {
    out << "(try " << body_;
    
    for (int i = 0; i < catches_.count(); i++)
    {
      out << " (catch " << catches_[i].pattern() << " then ";
      out << catches_[i].body() << ")";
    }
    
    out << ")";
  }
  
  void DefMethodNode::trace(std::ostream& out) const
  {
    out << "(def ";
    if (!leftParam_.isNull())
    {
      out << "(" << leftParam_ << ") ";
    }

    out << name_;
    
    if (!rightParam_.isNull())
    {
      out << " (" << rightParam_ << ")";
    }
    
    out << " -> " << body_ << ")";
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
  
  void NotNode::trace(std::ostream& out) const
  {
    out << "(not " << value_ << ")";
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
  
  void RecordNode::trace(std::ostream& out) const
  {
    out << "(";
    
    for (int i = 0; i < fields_.count(); i++)
    {
      if (i > 0) out << ", ";
      out << fields_[i].name << ": " << fields_[i].value;
    }
    
    out << ")";
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
    out << "(\n";
    
    for (int i = 0; i < expressions_.count(); i++)
    {
      out << expressions_[i] << "\n";
    }
    
    out << ")";
  }
  
  void StringNode::trace(std::ostream& out) const
  {
    out << "\"" << value_ << "\"";
  }
  
  void ThrowNode::trace(std::ostream& out) const
  {
    out << "(throw " << value_ << ")";
  }
  
  void VariableNode::trace(std::ostream& out) const
  {
    out << "(" << (isMutable_ ? "var " : "val ");
    out << pattern_ << " = " << value_ << ")";
  }
  
  void NothingPattern::trace(std::ostream& out) const
  {
    out << "nothing";
  }
  
  void RecordPattern::trace(std::ostream& out) const
  {
    out << "(";
    
    for (int i = 0; i < fields_.count(); i++)
    {
      if (i > 0) out << ", ";
      out << fields_[i].name << ": " << fields_[i].value;
    }
    
    out << ")";
  }
  
  void TypePattern::trace(std::ostream& out) const
  {
    out << "(is " << type_ << ")";
  }
  
  void ValuePattern::trace(std::ostream& out) const
  {
    out << "(== " << value_ << ")";
  }
  
  void VariablePattern::trace(std::ostream& out) const
  {
    if (pattern_.isNull())
    {
      out << "(" << name_ << " " << pattern_ << ")";
    }
    else
    {
      out << name_;
    }
  }
}

