#include "Compiler.h"
#include "Method.h"
#include "Node.h"
#include "Object.h"

namespace magpie
{
  temp<Method> Compiler::compileMethod(const Node& node)
  {
    Compiler compiler;
    return compiler.compile(node);
  }
  
  Compiler::Compiler()
  : NodeVisitor(),
    numInUseRegisters_(0),
    maxRegisters_(0)
  {}

  temp<Method> Compiler::compile(const Node& node)
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
    
    return Method::create(code_, constants_, maxRegisters_);
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
  
  void Compiler::visit(const IfNode& node, int dest)
  {
    // Compile the condition.
    node.condition().accept(*this, dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToElse = startJump();
    
    // Compile the then arm.
    node.thenArm().accept(*this, dest);
    
    // Leave a space for the then arm to jump over the else arm.
    int jumpPastElse = startJump();
    
    // Compile the else arm.
    endJump(jumpToElse, OP_JUMP_IF_ZERO, dest, code_.count() - jumpToElse - 1);
      
    node.elseArm().accept(*this, dest);
      
    endJump(jumpPastElse, OP_JUMP, code_.count() - jumpPastElse - 1);
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
    constants_.add(constant);
    return constants_.count() - 1;
  }
  
  void Compiler::write(OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);
    
    code_.add(MAKE_ABC(a, b, c, op));
  }

  int Compiler::startJump()
  {
    // Just write a dummy op to leave a space for the jump instruction.
    write(OP_MOVE);
    return code_.count() - 1;
  }
  
  void Compiler::endJump(int from, OpCode op, int a, int b, int c)
  {
    code_[from] = MAKE_ABC(a, b, c, op);
  }

  int Compiler::reserveRegister()
  {
    numInUseRegisters_++;
    
    if (maxRegisters_ < numInUseRegisters_)
    {
      maxRegisters_ = numInUseRegisters_;
    }
    
    return numInUseRegisters_ - 1;
  }
  
  void Compiler::releaseRegister()
  {
    ASSERT(numInUseRegisters_ > 0, "Released too many registers.");
    numInUseRegisters_--;
  }
}