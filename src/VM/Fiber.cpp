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
    // TODO(bob): What should the arg object be here?
    call(method, 0, gc<Object>());
    return run();
  }

  temp<Object> Fiber::run()
  {
    int ip = 0;
    bool running = true;
    while (running)
    {
      AllocScope scope;
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
          
        case OP_BOOL:
        {
          bool value = GET_A(ins) == 1;
          int reg = GET_B(ins);
          // TODO(bob): Should just create singleton instances of true and false
          // and reuse them.
          store(frame, reg, vm_.getBool(value));
          break;
        }
          
        case OP_CALL:
        {
          // Store the IP back into the callframe so we know where to resume
          // when we return to it.
          frame.ip = ip;
          ip = 0;
          
          gc<Method> method = vm_.globals().get(GET_A(ins));
          gc<Object> arg = load(frame, GET_B(ins));
          call(method, stack_.count() - 1, arg);
          break;
        }
        
        case OP_END:
        {
          gc<Object> result = loadRegisterOrConstant(frame, GET_A(ins));
          callFrames_.remove(-1);

          if (callFrames_.count() > 0)
          {
            // Give the result back and resume the calling method.
            CallFrame& caller = callFrames_[-1];
            ip = caller.ip;

            instruction callInstruction = caller.method->code()[ip - 1];
            ASSERT(GET_OP(callInstruction) == OP_CALL,
                   "Should be returning to a call.");
            
            store(caller, GET_B(callInstruction), result);
          }
          else
          {
            // The last method has returned, so end the fiber.
            running = false;
            return result.toTemp();
          }
          break;
        }
          
        case OP_ADD:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() + b->toNumber();
          temp<Object> num = Object::create(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_SUBTRACT:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() - b->toNumber();
          temp<Object> num = Object::create(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_MULTIPLY:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() * b->toNumber();
          temp<Object> num = Object::create(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_DIVIDE:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() / b->toNumber();
          temp<Object> num = Object::create(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_LESS_THAN:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          bool c = a->toNumber() < b->toNumber();
          store(frame, GET_C(ins), vm_.getBool(c));
          break;
        }
          
        case OP_JUMP:
        {
          int offset = GET_A(ins);
          ip += offset;
          break;
        }
          
        case OP_JUMP_IF_FALSE:
        {
          gc<Object> a = load(frame, GET_A(ins));
          if (!a->toBool())
          {
            int offset = GET_B(ins);
            ip += offset;
          }
          break;
        }
          
        default:
          ASSERT(false, "Unknown opcode.");
          break;
      }
    }
    
    ASSERT(false, "Should not get here.");
    return temp<Object>();
  }

  void Fiber::call(gc<Method> method, int stackStart, gc<Object> arg)
  {
    //std::cout << "call " << method->name() << std::endl;
    
    // Allocate registers for the method.
    // TODO(bob): Make this a single operation on Array.
    while (stack_.count() < stackStart + method->numRegisters())
    {
      stack_.add(gc<Object>());
    }
    
    // Bind the argument in the called method.
    stack_[stackStart] = arg;
    
    callFrames_.add(CallFrame(method, stackStart));
  }

  
  gc<Object> Fiber::loadRegisterOrConstant(const CallFrame& frame, int index)
  {
    if (IS_CONSTANT(index))
    {
      return frame.method->getConstant(GET_CONSTANT(index));
    }
    else
    {
      return load(frame, index);
    }
  }
}