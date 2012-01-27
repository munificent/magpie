#pragma once

#define MAKE_ABC(a, b, c, op)          ((a << 24) | (b << 16) | (c << 8) | op)
#define MAKE_AxC(a, c, op)             ((a << 16) | (c << 8) | op)

// Macros for destructuring instructions.

#define GET_OP(i) (static_cast<OpCode>((i) & 0xff))
#define GET_A(i)  (static_cast<int>(((i) & 0xff000000) >> 24))
#define GET_B(i)  (static_cast<int>(((i) & 0x00ff0000) >> 16))
#define GET_C(i)  (static_cast<int>(((i) & 0x0000ff00) >>  8))

#define GET_Ax(i) (static_cast<int>(((i) & 0xffff0000) >> 16))

namespace magpie
{
  enum OpCode
  {
    OP_MOVE       = 0x01, // A: from, B: to
    OP_CONSTANT   = 0x02, // A: index, B: dest register
    OP_CALL       = 0x03, // A: arg, B: method, C: result
    OP_END        = 0x04, // A: result register
    OP_HACK_PRINT = 0x05, // A: register
  };
  
  typedef unsigned int instruction;
}