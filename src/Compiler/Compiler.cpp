#include "Compiler.h"
#include "Method.h"
#include "Node.h"
#include "Object.h"

namespace magpie
{
  temp<Method> Compiler::compileMethod(const Node& node)
  {
    AllocScope scope;
    
    temp<Method> method = Method::create();
    
    Compiler compiler(method);
    
    compiler.compile(node);
    
    return scope.close(method);
  }
  
  Compiler::Compiler(temp<Method> method)
  : NodeVisitor(),
    method_(method),
    numInUseRegisters_(0)
  {}

  void Compiler::compile(const Node& node)
  {
    /*
    // Reserve registers for the params. These have to go first because the
    // caller will place them here.
    for (int i = 0; i < params.Count(); i++)
    {
      ReserveRegister();
      mLocals.Add(params[i]);
    }
    */
    
    int result = compileExpressionOrConstant(node);
    write(OP_END, result);
  }

  void Compiler::visit(const BinaryOpNode& node, int dest)
  {
    switch (node.type())
    {
      case TOKEN_PLUS:  compileInfix(node, OP_ADD, dest); break;
      case TOKEN_MINUS: compileInfix(node, OP_SUBTRACT, dest); break;
      case TOKEN_STAR:  compileInfix(node, OP_MULTIPLY, dest); break;
      case TOKEN_SLASH: compileInfix(node, OP_DIVIDE, dest); break;
        
      default:
        ASSERT(false, "Unknown infix operator.");
    }
  }
  
  void Compiler::visit(const NumberNode& node, int dest)
  {
    int index = compileConstant(node);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::compileInfix(const BinaryOpNode& node, OpCode op, int dest)
  {
    int a = compileExpressionOrConstant(node.left());
    int b = compileExpressionOrConstant(node.right());
    
    write(op, a, b, dest);
    
    if (IS_REGISTER(a)) releaseRegister();
    if (IS_REGISTER(b)) releaseRegister();
  }
  
  int Compiler::compileExpressionOrConstant(const Node& node)
  {
    const NumberNode* number = node.asNumberNode();
    if (number == NULL)
    {
      int dest = reserveRegister();
      node.accept(*this, dest);
      return dest;
    }
    else
    {
      return MAKE_CONSTANT(compileConstant(*number));
    }
  }
  
  int Compiler::compileConstant(const NumberNode& node)
  {
    temp<NumberObject> constant = Object::create(node.value());
    
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    return method_->addConstant(constant);
  }
  
  void Compiler::write(OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);
    
    method_->write(MAKE_ABC(a, b, c, op));
  }

  
  int Compiler::reserveRegister()
  {
    numInUseRegisters_++;
    
    if (method_->numRegisters() < numInUseRegisters_)
    {
      method_->setNumRegisters(numInUseRegisters_);
    }
    
    return numInUseRegisters_ - 1;
  }
  
  void Compiler::releaseRegister()
  {
    ASSERT(numInUseRegisters_ > 0, "Released too many registers.");
    numInUseRegisters_--;
  }
}