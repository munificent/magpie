#include "Fiber.h"

#include "Method.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  Fiber::Fiber(VM& vm)
  : vm_(vm),
    stack_(),
    callFrames_()
  {}

  void Fiber::init(gc<Method> method)
  {
    ASSERT(stack_.count() == 0, "Cannot re-initialize Fiber.");
    ASSERT(callFrames_.count() == 0, "Cannot re-initialize Fiber.");

    // TODO(bob): What should the arg object be here?
    call(method, 0, vm_.nothing());
  }

  gc<Object> Fiber::run()
  {
    while (true)
    {
      if (Memory::checkCollect()) return gc<Object>();
      
      CallFrame& frame = callFrames_[-1];
      instruction ins = frame.method->code()[frame.ip++];
      OpCode op = GET_OP(ins);

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
          
        case OP_BUILT_IN:
        {
          int value = GET_A(ins);
          int reg = GET_B(ins);
          store(frame, reg, vm_.getBuiltIn(value));
          break;
        }
          
        case OP_RECORD:
        {
          int firstReg = GET_A(ins);
          gc<RecordType> type = vm_.getRecordType(GET_B(ins));
          gc<Object> record = RecordObject::create(type, stack_, firstReg);
          store(frame, GET_C(ins), record);
          break;
        }
          
        case OP_DEF_METHOD:
        {
          gc<Method> method = frame.method->getMethod(GET_A(ins));
          vm_.methods().define(GET_B(ins), method);
          break;
        }
          
        case OP_GET_FIELD:
        {
          RecordObject* record = load(frame, GET_A(ins))->toRecord();
          ASSERT(record != NULL, "Need to implement trying to get a field from a non-record.");
          
          int symbol = GET_B(ins);
          store(frame, GET_C(ins), record->getField(symbol));
          break;
        }
          
        case OP_CALL:
        {
          gc<Method> method = vm_.methods().get(GET_A(ins));
          gc<Object> arg = load(frame, GET_B(ins));
          
          Primitive primitive = method->primitive();
          if (primitive != NULL) {
            gc<Object> result = primitive(arg);
            store(frame, GET_B(ins), result);
          } else {
            int stackStart = frame.stackStart + frame.method->numRegisters();
            call(method, stackStart, arg);
          }
          break;
        }
        
        case OP_RETURN:
        {
          gc<Object> result = loadRegisterOrConstant(frame, GET_A(ins));
          callFrames_.removeAt(-1);

          if (callFrames_.count() > 0)
          {
            // Give the result back and resume the calling method.
            CallFrame& caller = callFrames_[-1];
            instruction callInstruction = caller.method->code()[caller.ip - 1];
            ASSERT(GET_OP(callInstruction) == OP_CALL,
                   "Should be returning to a call.");
            
            store(caller, GET_B(callInstruction), result);
          }
          else
          {
            // The last method has returned, so end the fiber.
            return result;
          }
          break;
        }
          
        case OP_ADD:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() + b->toNumber();
          gc<Object> num = new NumberObject(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_SUBTRACT:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() - b->toNumber();
          gc<Object> num = new NumberObject(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_MULTIPLY:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() * b->toNumber();
          gc<Object> num = new NumberObject(c);
          store(frame, GET_C(ins), num);
          break;
        }
          
        case OP_DIVIDE:
        {
          gc<Object> a = loadRegisterOrConstant(frame, GET_A(ins));
          gc<Object> b = loadRegisterOrConstant(frame, GET_B(ins));
          
          // TODO(bob): Handle non-number types.
          double c = a->toNumber() / b->toNumber();
          gc<Object> num = new NumberObject(c);
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
          
        case OP_NOT:
        {
          gc<Object> value = loadRegisterOrConstant(frame, GET_A(ins));
          
          // TODO(bob): Handle user-defined types.
          bool result = !value->toBool();
          store(frame, GET_A(ins), vm_.getBool(result));
          break;
        }
          
        case OP_JUMP:
        {
          int offset = GET_A(ins);
          frame.ip += offset;
          break;
        }
          
        case OP_JUMP_IF_FALSE:
        {
          gc<Object> a = load(frame, GET_A(ins));
          if (!a->toBool())
          {
            int offset = GET_B(ins);
            frame.ip += offset;
          }
          break;
        }
          
        case OP_JUMP_IF_TRUE:
        {
          gc<Object> a = load(frame, GET_A(ins));
          if (a->toBool())
          {
            int offset = GET_B(ins);
            frame.ip += offset;
          }
          break;
        }
          
        default:
          ASSERT(false, "Unknown opcode.");
          break;
      }
    }
    
    ASSERT(false, "Should not get here.");
    return gc<Object>();
  }
  
  void Fiber::reach()
  {
    // Only reach registers that are still in use. We don't shrink the stack,
    // so it may have dead registers at the end that are safe to collect.
    CallFrame& frame = callFrames_[-1];
    int numRegisters = frame.stackStart + frame.method->numRegisters();
    for (int i = 0; i < numRegisters; i++)
    {
      Memory::reach(stack_[i]);
    }
    
    for (int i = 0; i < callFrames_.count(); i++)
    {
      Memory::reach(callFrames_[i].method);
    }
  }
  
  void Fiber::call(gc<Method> method, int stackStart, gc<Object> arg)
  {
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