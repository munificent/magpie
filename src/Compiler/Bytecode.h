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
    // Moves the value in register A to register B.
    OP_MOVE = 0x01,
    
    // Loads the constant with index A into register B.
    OP_CONSTANT = 0x02,
    
    // Loads the built-in value with index A (see VM::getBuiltIn()) into
    // register B.
    OP_BUILT_IN = 0x03,
    
    // Creates a record from fields on the stack. A is the register of the
    // first field, with subsequent fields following on the stack. B is the
    // index of the record type (see VM::getRecordType()). Stores the record in
    // register C.
    OP_RECORD = 0x04,
    
    // Defines a new top-level global method. A is the index of the new method
    // in the containing method's list of methods. B is the index of the method
    // in the VM's method table.
    OP_DEF_METHOD = 0x05,
    
    // Destructures a record field. Register A holds the record to destructure.
    // B is the symbol for the field (see VM::addSymbol()). Stores the field
    // value in register C.
    OP_GET_FIELD = 0x06,
    
    // Loads a top-level variable exported from a module. A is the index of
    // the imported module in the containing module's import list. B is the
    // index of the exported variable in that module to load. Stores the value
    // in register C.
    OP_GET_MODULE = 0x07,
    
    OP_ADD           = 0x08, // R(C) = RC(A) + RC(B)
    OP_SUBTRACT      = 0x09, // R(C) = RC(A) - RC(B)
    OP_MULTIPLY      = 0x0a, // R(C) = RC(A) * RC(B)
    OP_DIVIDE        = 0x0b, // R(C) = RC(A) / RC(B)
    OP_LESS_THAN     = 0x0c, // R(C) = RC(A) < RC(B)
    OP_NOT           = 0x0d, // R(C) = RC(A) + RC(B)
    OP_JUMP          = 0x0e, // A = offset
    OP_JUMP_IF_FALSE = 0x0f, // R(A) = test register, B = offset
    OP_JUMP_IF_TRUE  = 0x10, // R(A) = test register, B = offset
    OP_CALL          = 0x11, // A: method, B: arg and result
    
    // Exits the current method, returning register A.
    OP_RETURN = 0x12,
    
    // Throws the error object in register A.
    OP_THROW = 0x13
  };
  
  enum BuiltIn
  {
    BUILT_IN_FALSE   = 0,
    BUILT_IN_TRUE    = 1,
    BUILT_IN_NOTHING = 2
  };
  
  typedef unsigned int instruction;
}