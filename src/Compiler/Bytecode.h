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
    OP_CONSTANT,
    
    // Loads the built-in value with index A (see VM::getBuiltIn()) into
    // slot B.
    OP_BUILT_IN,
    
    // Adds a method to a multimethod. A is the index of the multimethod to
    // specialize. B is the index of the method to add.
    OP_METHOD,
    
    // Creates a record from fields on the stack. A is the slot of the first
    // field, with subsequent fields following on the stack. B is the index of
    // the record type (see VM::getRecordType()). Stores the record in slot C.
    OP_RECORD,
    
    // Creates a list from elements on the stack. A is the slot of the first
    // element, B is the number of elements. The resulting list is placed in
    // slot C.
    OP_LIST,

    // Creates a function. A is constant index of the chunk for the body. Stores
    // the result in slot B.
    OP_FUNCTION,
    
    // Creates an async fiber. A is constant index of the chunk for the fiber.
    OP_ASYNC,

    // Creates a class. A is the symbol ID for the name. B is the number of
    // fields. Stores the result in slot C.
    // TODO(bob): The destination slot will almost never be needed since classes
    // usually appear in "statement" position. Can it be eliminated?
    OP_CLASS,

    // Destructures a record field. Slot A holds the record to destructure. B
    // is the symbol for the field (see VM::addSymbol()). Stores the field
    // value in slot C. If slot A does not have a record, or the record does
    // not have the expected field, throws a NoMatchError.
    OP_GET_FIELD,
    
    // Similar to OP_GET_FIELD. However, if the match fails, it does not throw.
    // Instead, the instruction following this one is required to be an OP_JUMP
    // containing the offset to jump to.
    OP_TEST_FIELD,

    // Accesses a class field. A is the index of the field. This opcode should
    // only appear inside auto-generated methods because it assumes a certain
    // slot layout.
    OP_GET_CLASS_FIELD,

    // Assigns a class field. A is the index of the field. This opcode should
    // only appear inside auto-generated methods because it assumes a certain
    // slot layout.
    OP_SET_CLASS_FIELD,
    
    // Loads a top-level variable from a module. A is the index of the module
    // in the VM's global module list. B is the index of the variable in that
    // module to load. Stores the value in slot C.
    OP_GET_VAR,
    
    // Sets a top-level variable. A is the index of the module in the VM's
    // global module list. B is the index of the variable in that module to set.
    // Slot C is the value to store.
    OP_SET_VAR,

    // Loads the value of an upvar. A is the index of the upvar. Stores the
    // value in slot B.
    OP_GET_UPVAR,

    // Sets an upvar. A is the index of the upvar. Gets the value from slot B.
    // If C is 1 then a new upvar is created, otherwise, the existing one is
    // assigned.
    OP_SET_UPVAR,
    
    OP_EQUAL, // R(C) = RC(A) == RC(B)
    OP_NOT, // R(C) = RC(A) + RC(B)
    
    // Tests if the value in slot A is an instance of the type in slot B.
    // Stores the result in slot C.
    OP_IS,
    
    // Performs an unconditional jump. If A is 1, then the instruction pointer
    // is moved forward by B. Otherwise, it is moved back by that amount.
    OP_JUMP,
    
    OP_JUMP_IF_FALSE, // R(A) = test slot, B = offset
    OP_JUMP_IF_TRUE, // R(A) = test slot, B = offset
    
    // Invokes a top-level method. The index of the method in the global table
    // is A. The arguments to the method are laid out in sequential slots
    // starting at B. The number of slots needed is determined by the
    // signature, so is not explicitly passed. The result will be stored in
    // slot C when the method returns.
    // TODO(bob): Tweak operands so that we can support more than 256 methods.
    OP_CALL,
    
    // Invokes a native method. The index of the native is A. The result of the
    // call will be placed into register C. Assumes the arguments to the
    // native are the top of the current call frame's stack.
    OP_NATIVE,
    
    // Exits the current method, returning slot A.
    OP_RETURN,
    
    // Throws the error object in slot A.
    OP_THROW,
    
    // Registers a new catch handler. If an error is thrown before the
    // subsequent OP_EXIT_TRY, then execution will jump to the associated catch
    // block. Its code location is the location of the OP_ENTER_TRY + A.
    OP_ENTER_TRY,
    
    // Discards the previous OP_ENTER_TRY handler. This occurs when execution
    // has proceeded past the block containing a catch clause.
    OP_EXIT_TRY,
    
    // Throws a NoMatchError if slot A is false.
    OP_TEST_MATCH
  };
  
  enum BuiltIn
  {
    BUILT_IN_FALSE     = 0,
    BUILT_IN_TRUE      = 1,
    BUILT_IN_NOTHING   = 2,
    BUILT_IN_NO_METHOD = 3,
    BUILT_IN_DONE      = 4
  };
  
  typedef unsigned int instruction;
}