#include "Compiler.h"
#include "Method.h"
#include "Node.h"
#include "NumberObject.h"
#include "Object.h"

namespace magpie
{
  temp<Method> Compiler::compileMethod(const Node& node)
  {
    AllocScope scope;
    
    temp<Method> method = Method::create();
    
    Compiler compiler(method);
    
    // TODO(bob): Reserve a real register for the return.
    node.accept(compiler, 1);
    
    return scope.close(method);
  }
  
  Compiler::Compiler(temp<Method> method)
  : NodeVisitor(),
    method_(method)
  {}

  void Compiler::visit(const BinaryOpNode& node, int dest)
  {
    ASSERT(false, "Not implemented yet.");
  }
  
  void Compiler::visit(const NumberNode& node, int dest)
  {
    temp<NumberObject> constant = Object::create(node.value());
    compileConstant(constant, dest);
  }

  void Compiler::compileConstant(temp<Managed> constant, int dest)
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
}