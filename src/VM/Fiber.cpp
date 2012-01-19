#include "Fiber.h"

#include "Chunk.h"
#include "NumberObject.h"
#include "VM.h"

namespace magpie {

  Fiber::Fiber(VM& vm)
  : vm_(vm) {
  }
  
  void Fiber::interpret(gc<Chunk> chunk) {
    call(chunk, gc<Object>());
    run();
  }
  
  unsigned short Fiber::addLiteral(gc<Object> value) {
    literals_.add(value);
    return literals_.count() - 1;
  }

  void Fiber::run() {
    int ip = 0;
    bool running = true;
    while (running) {
      CallFrame& frame = *stack_[-1];
      unsigned int instruction = (*frame.getChunk())[ip];
      unsigned char op = GET_OP(instruction);
      ip++;
      
      switch (op) {
        case OP_MOVE: {
          unsigned char from = GET_A(instruction);
          unsigned char to = GET_B(instruction);
          frame.setRegister(to, frame.getRegister(from));
          break;
        }
          
        case OP_LITERAL: {
          unsigned short index = GET_Ax(instruction);
          unsigned char reg = GET_C(instruction);
          frame.setRegister(reg, literals_[index]);
          break;
        }
          
        case OP_CALL: {
          unsigned char argReg = GET_A(instruction);
          unsigned char methodReg = GET_B(instruction);
          
          gc<Object> arg = frame.getRegister(argReg);
          gc<Object> methodObj = frame.getRegister(methodReg);
          Multimethod* multimethod = frame.getRegister(methodReg)->asMultimethod();
          
          gc<Chunk> method = multimethod->select(arg);
          // TODO(bob): Handle method not found.
          
          // Store the IP back into the callframe so we know where to resume
          // when we return to it.
          frame.setInstruction(ip - 1);
          ip = 0;
          
          call(method, arg);
          break;
        }
        
        case OP_RETURN: {
          unsigned char reg = GET_A(instruction);
          
          gc<Object> result = frame.getRegister(reg);
          stack_.remove(-1);
          
          if (stack_.count() > 0) {
            // Give the result back and resume the calling method.
            CallFrame& caller = *stack_[-1];
            ip = caller.getInstruction();

            unsigned int callInstruction = (*caller.getChunk())[ip];
            unsigned char reg = GET_C(callInstruction);
            caller.setRegister(reg, result);
            
            // Done with the CALL that we're returning from.
            ip++;
          } else {
            // The last method has returned, so end the fiber.
            running = false;
          }
          break;
        }
          
        case OP_HACK_PRINT: {
          unsigned char reg = GET_A(instruction);

          gc<Object> object = frame.getRegister(reg);
          std::cout << "Hack print: " << object << "\n";
          break;
        }
          
        default:
          ASSERT(false, "Unknown opcode.");
          break;
      }
    }
  }
  
  void Fiber::call(gc<Chunk> chunk, gc<Object> arg) {
    stack_.add(gc<CallFrame>(new CallFrame(chunk)));
    
    // The argument always goes into the first register.
    stack_[-1]->setRegister(0, arg);
  }
}