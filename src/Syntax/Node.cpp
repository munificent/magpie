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

  void NumberNode::trace(std::ostream& out) const
  {
    out << value_;
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

