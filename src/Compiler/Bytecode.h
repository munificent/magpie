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
#define IS_SLOT(i) (((i) & 0x80) == 0x00)

#define GET_CONSTANT(i) ((i) & 0x7f)
#define MAKE_CONSTANT(i) ((i) | 0x80)

namespace magpie
{
  enum OpCode
  {
    // Moves the value in slot A to slot B.
    OP_MOVE = 0x01,
    
    // Loads the constant with index A into slot B.
    OP_CONSTANT = 0x02,
    
    // Loads the built-in value with index A (see VM::getBuiltIn()) into
    // slot B.
    OP_BUILT_IN = 0x03,
    
    // Adds a method to a multimethod. A is the index of the multimethod to
    // specialize. B is the index of the method to add.
    OP_METHOD = 0x04,
    
    // Creates a record from fields on the stack. A is the slot of the first
    // field, with subsequent fields following on the stack. B is the index of
    // the record type (see VM::getRecordType()). Stores the record in slot C.
    OP_RECORD = 0x05,
        
    // Destructures a record field. Slot A holds the record to destructure. B
    // is the symbol for the field (see VM::addSymbol()). Stores the field
    // value in slot C. If slot A does not have a record, or the record does
    // not have the expected field, throws a NoMatchError.
    OP_GET_FIELD = 0x06,
    
    // Similar to OP_GET_FIELD. However, if the match fails, it does not throw.
    // Instead, the instruction following this one is required to be an OP_JUMP
    // containing the offset to jump to.
    OP_TEST_FIELD = 0x07,
    
    // Loads a top-level variable exported from a module. A is the index of
    // the module in the VM's global module list. B is the index of the
    // exported variable in that module to load. Stores the value in slot C.
    OP_GET_MODULE = 0x08,

    OP_EQUAL         = 0x09, // R(C) = RC(A) == RC(B)
    OP_LESS_THAN     = 0x0a, // R(C) = RC(A) < RC(B)
    OP_GREATER_THAN  = 0x0b, // R(C) = RC(A) > RC(B)
    OP_NOT           = 0x0c, // R(C) = RC(A) + RC(B)
    
    // Tests if the value in slot A is an instance of the type in slot B.
    // Stores the result in slot C.
    OP_IS = 0x0d,
    
    // Performs an unconditional jump. If A is 1, then the instruction pointer
    // is moved forward by B. Otherwise, it is moved back by that amount.
    OP_JUMP = 0x0e,
    
    OP_JUMP_IF_FALSE = 0x0f, // R(A) = test slot, B = offset
    OP_JUMP_IF_TRUE  = 0x10, // R(A) = test slot, B = offset
    
    // Invokes a top-level method. The index of the method in the global table
    // is A. The arguments to the method are laid out in sequential slots
    // starting at B. The number of slots needed is determined by the
    // signature, so is not explicitly passed. The result will be stored in
    // slot C when the method returns.
    // TODO(bob): Tweak operands so that we can support more than 256 methods.
    OP_CALL = 0x11,
    
    // Invokes a native method. The index of the native is A. The result of the
    // call will be placed into register B. Assumes the arguments to the
    // native are the top of the current call frame's stack.
    OP_NATIVE = 0x12,
    
    // Exits the current method, returning slot A.
    OP_RETURN = 0x13,
    
    // Throws the error object in slot A.
    OP_THROW = 0x14,
    
    // Registers a new catch handler. If an error is thrown before the
    // subsequent OP_EXIT_TRY, then execution will jump to the associated catch
    // block. Its code location is the location of the OP_ENTER_TRY + A.
    OP_ENTER_TRY = 0x15,
    
    // Discards the previous OP_ENTER_TRY handler. This occurs when execution
    // has proceeded past the block containing a catch clause.
    OP_EXIT_TRY = 0x16,
    
    // Throws a NoMatchError if slot A is false.
    OP_TEST_MATCH = 0x17
  };
  
  enum BuiltIn
  {
    BUILT_IN_FALSE   = 0,
    BUILT_IN_TRUE    = 1,
    BUILT_IN_NOTHING = 2
  };
  
  typedef unsigned int instruction;
}