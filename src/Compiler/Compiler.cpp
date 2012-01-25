#include "Compiler.h"
#include "Node.h"
#include "NumberObject.h"
#include "Object.h"

namespace magpie
{
  void Compiler::visit(BinaryOpNode& node, int dest)
  {
  }
  
  void Compiler::visit(NumberNode& node, int dest)
  {
    temp<NumberObject> constant = Object::create(node.value());
    compileConstant(constant, dest);
  }

  void Compiler::compileConstant(temp<Managed> constant, int dest)
  {
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    constants_.add(constant);
    write(OP_CONSTANT, constants_.count() - 1, dest);
  }

  void Compiler::write(OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);
    
    code_.add(MAKE_ABC(a, b, c, op));
  }
}