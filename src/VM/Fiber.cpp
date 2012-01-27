#include "Fiber.h"

#include "Method.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  temp<Fiber> Fiber::create(VM& vm)
  {
    return Memory::makeTemp(new Fiber(vm));
  }
  
  Fiber::Fiber(VM& vm)
  : vm_(vm),
    stack_(),
    callFrames_()
  {}

  temp<Object> Fiber::interpret(gc<Method> method)
  {
    call(method, 0);
    return run();
  }

  temp<Object> Fiber::run()
  {
    int ip = 0;
    bool running = true;
    while (running)
    {
      CallFrame& frame = callFrames_[-1];
      instruction ins = frame.method->code()[ip];
      OpCode op = GET_OP(ins);
      ip++;

      switch (op)
      {
        case OP_MOVE:
        {
          int from = GET_A(ins);
          int to = GET_B(ins);
          store(frame, to, load(frame, from));
          break;
        }

        case OP_CONSTANT:
        {
          int index = GET_A(ins);
          int reg = GET_B(ins);
          store(frame, reg, frame.method->getConstant(index));
          break;
        }

          /*
        case OP_CALL:
        {
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
           */
          
        case OP_END:
        {
          unsigned char reg = GET_A(ins);

          gc<Object> result = load(frame, reg);
          callFrames_.remove(-1);

          if (callFrames_.count() > 0)
          {
            // Give the result back and resume the calling method.
            CallFrame& caller = callFrames_[-1];
            ip = caller.ip;

            instruction callInstruction = caller.method->code()[ip - 1];
            ASSERT(GET_OP(callInstruction) == OP_CALL,
                   "Should be returning to a call.");
            
            int reg = GET_C(callInstruction);
            store(caller, reg, result);
          }
          else
          {
            // The last method has returned, so end the fiber.
            running = false;
            return result.toTemp();
          }
          break;
        }
/*
        case OP_HACK_PRINT:
        {
          unsigned char reg = GET_A(instruction);

          gc<Object> object = frame.getRegister(reg);
          std::cout << "Hack print: " << object << "\n";
          break;
        }
*/
        default:
          ASSERT(false, "Unknown opcode.");
          break;
      }
    }
    
    ASSERT(false, "Should not get here.");
    return temp<Object>();
  }

  void Fiber::call(gc<Method> method, int stackStart)
  {
    // Allocate registers for the method.
    // TODO(bob): Make this a single operation on Array.
    while (stack_.count() < stackStart + method->numRegisters())
    {
      stack_.add(gc<Object>());
    }
    
    callFrames_.add(CallFrame(method, stackStart));
  }
}