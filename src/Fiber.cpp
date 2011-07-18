#include "Fiber.h"

#include "Chunk.h"
#include "NumberObject.h"

namespace magpie {

  void Fiber::interpret(Ref<Chunk> chunk) {
    call(chunk, Ref<Object>());
    run();
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
          unsigned char from = (instruction & 0x00ff0000) >> 16;
          unsigned char to = (instruction & 0x0000ff00) >> 8;
          frame.setRegister(to, frame.getRegister(from));
          break;
        }
          
        case OP_LOAD_SHORT: {
          unsigned short value = (instruction & 0xffff0000) >> 16;
          unsigned char reg = (instruction & 0x0000ff00) >> 8;
          frame.setRegister(reg, Object::create((double)value));
          break;
        }
          
        case OP_CALL: {
          unsigned char argReg = (instruction & 0x00ff0000) >> 16;
          unsigned char methodReg = (instruction & 0x0000ff00) >> 8;
          Ref<Object> arg = frame.getRegister(argReg);
          Multimethod* multimethod = frame.getRegister(methodReg)->asMultimethod();
          
          Ref<Chunk> method = multimethod->select(arg);
          // TODO(bob): Handle method not found.
          
          frame.setInstruction(ip - 1);
          
          call(method, arg);
          break;
        }
        
        case OP_RETURN: {
          unsigned char reg = (instruction & 0x0000ff00) >> 8;
          return_ = frame.getRegister(reg);
          stack_.remove(-1);
          
          if (stack_.count() > 0) {
            // Give the result back and resume the calling method.
            CallFrame& caller = *stack_[-1];
            ip = caller.getInstruction();
            
            unsigned int callInstruction = (*frame.getChunk())[ip];
            unsigned char reg = (callInstruction & 0xff000000) >> 24;
            caller.setRegister(reg, return_);
          } else {
            // The last method has returned, so end the fiber.
            running = false;
          }
          break;
        }
          
        case OP_HACK_PRINT: {
          unsigned char reg = (instruction & 0x0000ff00) >> 8;
          Ref<Object> object = frame.getRegister(reg);
          double value = object->asNumber()->getValue();
          std::cout << "Register " << (int)reg << " = " << value << "\n";
          break;
        }
      }
    }
  }
  
  void Fiber::call(Ref<Chunk> chunk, Ref<Object> arg) {
    stack_.add(Ref<CallFrame>(new CallFrame(chunk)));
    
    // The argument always goes into the first register.
    stack_[-1]->setRegister(0, arg);
  }
}