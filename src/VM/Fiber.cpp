#include "Fiber.h"

#include "Method.h"
#include "NumberObject.h"
#include "VM.h"

namespace magpie
{
  Fiber::Fiber(VM& vm)
  : vm_(vm),
    stack_(),
    callFrames_()
  {}

  void Fiber::interpret(gc<Method> method)
  {
    call(method);
    run();
  }

  void Fiber::run()
  {
    int ip = 0;
    bool running = true;
    while (running)
    {
      CallFrame& frame = callFrames_[-1];
      instruction ins = frame.method()->code()[ip];
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

          /*
        case OP_CONSTANT:
        {
          unsigned short index = GET_Ax(instruction);
          unsigned char reg = GET_C(instruction);
          frame.setRegister(reg, literals_[index]);
          break;
        }

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

        case OP_RETURN:
        {
          unsigned char reg = GET_A(instruction);

          gc<Object> result = frame.getRegister(reg);
          callFrames_.remove(-1);

          if (callFrames_.count() > 0)
          {
            // Give the result back and resume the calling method.
            CallFrame& caller = *callFrames_[-1];
            ip = caller.getInstruction();

            unsigned int callInstruction = (*caller.getChunk())[ip];
            unsigned char reg = GET_C(callInstruction);
            caller.setRegister(reg, result);

            // Done with the CALL that we're returning from.
            ip++;
          }
          else
          {
            // The last method has returned, so end the fiber.
            running = false;
          }
          break;
        }

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
  }

  void Fiber::call(gc<Method> method)
  {
    // TODO(bob): TEMP! Should not always use zero for stack start.
    callFrames_.add(CallFrame(method, 0));
  }
}