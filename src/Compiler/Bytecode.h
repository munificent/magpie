#pragma once

#define MAKE_ABC(a, b, c, op)          ((a << 24) | (b << 16) | (c << 8) | op)
#define MAKE_AxC(a, c, op)             ((a << 16) | (c << 8) | op)

// Macros for destructuring instructions.

#define GET_OP(i) (static_cast<OpCode>((i) & 0xff))
#define GET_A(i)  (static_cast<int>(((i) & 0xff000000) >> 24))
#define GET_B(i)  (static_cast<int>(((i) & 0x00ff0000) >> 16))
#define GET_C(i)  (static_cast<int>(((i) & 0x0000ff00) >>  8))

#define GET_Ax(i) (static_cast<int>(((i) & 0xffff0000) >> 16))

#define IS_CONSTANT(i) (((i) & 0x80) == 0x80)
#define IS_REGISTER(i) (((i) & 0x80) == 0x00)

#define GET_CONSTANT(i) ((i) & 0x7f)
#define MAKE_CONSTANT(i) ((i) | 0x80)

namespace magpie
{
  // C(A) -> operand A is an index into the constant table
  // R(A) -> operand A will be a register
  // RC(A) -> operator A will be a constant if the high bit is set or a
  //          register if not
  enum OpCode
  {
    OP_MOVE          = 0x01, // A: from, B: to
    OP_CONSTANT      = 0x02, // C(A) -> R(B)
    OP_BUILT_IN      = 0x03, // A -> R(B)
    OP_ADD           = 0x04, // R(C) = RC(A) + RC(B)
    OP_SUBTRACT      = 0x05, // R(C) = RC(A) - RC(B)
    OP_MULTIPLY      = 0x06, // R(C) = RC(A) * RC(B)
    OP_DIVIDE        = 0x07, // R(C) = RC(A) / RC(B)
    OP_LESS_THAN     = 0x08, // R(C) = RC(A) < RC(B)
    OP_JUMP          = 0x09, // A = offset
    OP_JUMP_IF_FALSE = 0x0a, // R(A) = test register, B = offset
    OP_CALL          = 0x0b, // A: method, B: arg and result
    OP_END           = 0x0c  // RC(A): result
  };
  
  enum BuiltIn
  {
    BUILT_IN_FALSE   = 0,
    BUILT_IN_TRUE    = 1,
    BUILT_IN_NOTHING = 2
  };
  
  typedef unsigned int instruction;
}