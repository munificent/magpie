#include "Ast.h"

namespace magpie
{  
  void AndExpr::trace(std::ostream& out) const
  {
    out << "(and " << left_ << " " << right_ << ")";
  }
  
  void AssignExpr::trace(std::ostream& out) const
  {
    out << "(" << pattern_ << " = " << value_ << ")";
  }
  
  void BinaryOpExpr::trace(std::ostream& out) const
  {
    out << "(" << left_ << " " << Token::typeString(type_)
        << " " << right_ << ")";
  }

  void BoolExpr::trace(std::ostream& out) const
  {
    out << (value_ ? "true" : "false");
  }
  
  void CallExpr::trace(std::ostream& out) const
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
  
  void CatchExpr::trace(std::ostream& out) const
  {
    out << "(try " << body_;
    
    for (int i = 0; i < catches_.count(); i++)
    {
      out << " (catch " << catches_[i].pattern() << " then ";
      out << catches_[i].body() << ")";
    }
    
    out << ")";
  }
    
  void DefExpr::trace(std::ostream& out) const
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
  
  void DefClassExpr::trace(std::ostream& out) const
  {
    out << "(defclass " << name_ << ")";
  }
  
  void DoExpr::trace(std::ostream& out) const
  {
    out << "(do " << body_ << ")";
  }
  
  void IfExpr::trace(std::ostream& out) const
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
  
  void IsExpr::trace(std::ostream& out) const
  {
    out << "(" << value_ << " is " << type_ << ")";
  }
    
  void MatchExpr::trace(std::ostream& out) const
  {
    out << "(match " << value_;
    
    for (int i = 0; i < cases_.count(); i++)
    {
      out << " (case " << cases_[i].pattern() << " then ";
      out << cases_[i].body() << ")";
    }
    
    out << ")";
  }
  
  void NameExpr::trace(std::ostream& out) const
  {
    out << name_;
  }
  
  void NativeExpr::trace(std::ostream& out) const
  {
    out << "(native " << name_ << ")";
  }
  
  void NotExpr::trace(std::ostream& out) const
  {
    out << "(not " << value_ << ")";
  }
  
  void NothingExpr::trace(std::ostream& out) const
  {
    out << "nothing";
  }
  
  void NumberExpr::trace(std::ostream& out) const
  {
    out << value_;
  }
  
  void OrExpr::trace(std::ostream& out) const
  {
    out << "(or " << left_ << " " << right_ << ")";
  }
  
  void RecordExpr::trace(std::ostream& out) const
  {
    out << "(";
    
    for (int i = 0; i < fields_.count(); i++)
    {
      if (i > 0) out << ", ";
      out << fields_[i].name << ": " << fields_[i].value;
    }
    
    out << ")";
  }
  
  void ReturnExpr::trace(std::ostream& out) const
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
  
  void SequenceExpr::trace(std::ostream& out) const
  {
    out << "(\n";
    
    for (int i = 0; i < expressions_.count(); i++)
    {
      out << expressions_[i] << "\n";
    }
    
    out << ")";
  }
  
  void StringExpr::trace(std::ostream& out) const
  {
    out << "\"" << value_ << "\"";
  }
  
  void ThrowExpr::trace(std::ostream& out) const
  {
    out << "(throw " << value_ << ")";
  }
  
  void VariableExpr::trace(std::ostream& out) const
  {
    out << "(" << (isMutable_ ? "var " : "val ");
    out << pattern_ << " = " << value_ << ")";
  }
  
  void WhileExpr::trace(std::ostream& out) const
  {
    out << "(while " << condition_ << " " << body_ << ")";
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
    if (!pattern_.isNull())
    {
      out << "(" << name_ << " " << pattern_ << ")";
    }
    else
    {
      out << name_;
    }
  }
  
  void WildcardPattern::trace(std::ostream& out) const
  {
    out << "_";
  }
}

