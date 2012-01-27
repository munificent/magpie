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
    
    // TODO(bob): Instead of having an explicit register for the return,
    // which is often redundant since the result will be in some previous
    // register anyway many times, maybe pass a special DONT_CARE register
    // and have the visitors know to provide a register as needed and then
    // set some mReturnRegister member variable that we then use for the
    // END instruction.
    // Result register goes after params.
    int resultRegister = reserveRegister();
    /*
    // TODO(bob): Hackish. Add a fake local for it so that the indices in
    // mLocals correctly map local names -> register.
    mLocals.Add("(return)");
    */
    
    node.accept(*this, resultRegister);

    write(OP_END, resultRegister);
  }

  void Compiler::visit(const BinaryOpNode& node, int dest)
  {
    ASSERT(false, "Not implemented yet.");
  }
  
  void Compiler::visit(const NumberNode& node, int dest)
  {
    temp<NumberObject> constant = Object::create(node.value());
    compileConstant(constant, dest);
  }

  void Compiler::compileConstant(temp<Object> constant, int dest)
  {
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    int index = method_->addConstant(constant);
    write(OP_CONSTANT, index, dest);
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