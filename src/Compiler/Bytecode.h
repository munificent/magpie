#pragma once

#define MAKE_ABC(a, b, c, op)          ((a << 24) | (b << 16) | (c << 8) | op)
#define MAKE_AxC(a, c, op)             ((a << 16) | (c << 8) | op)

// Macros for building instructions.

#define MAKE_MOVE(from, to)            MAKE_ABC(from, to, 0, OP_MOVE)
#define MAKE_CONSTANT(index, r)        MAKE_AxC(index, r, OP_CONSTANT)
#define MAKE_CALL(arg, method, result) MAKE_ABC(arg, method, result, OP_CALL)
#define MAKE_RETURN(result)            MAKE_ABC(result, 0, 0, OP_RETURN)
#define MAKE_HACK_PRINT(r)             MAKE_ABC(r, 0, 0, OP_HACK_PRINT)

// Macros for destructuring instructions.

#define GET_OP(i) ((unsigned char)(i) & 0xff)
#define GET_A(i)  ((unsigned char)(((i) & 0xff000000) >> 24))
#define GET_B(i)  ((unsigned char)(((i) & 0x00ff0000) >> 16))
#define GET_C(i)  ((unsigned char)(((i) & 0x0000ff00) >>  8))

#define GET_Ax(i) ((unsigned short)(((i) & 0xffff0000) >> 16))

namespace magpie
{
  enum OpCode
  {
    OP_MOVE       = 0x01, // A: from, B: to
    OP_CONSTANT   = 0x02, // AB: index, C: dest register
    OP_CALL       = 0x03, // A: arg, B: method, C: result
    OP_RETURN     = 0x04, // A: result
    OP_HACK_PRINT = 0x05, // A: register
  };
  
  typedef unsigned int instruction;
}