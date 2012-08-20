#include "Fiber.h"

#include "Method.h"
#include "Module.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  Fiber::Fiber(VM& vm)
  : vm_(vm),
    stack_(),
    callFrames_(),
    nearestCatch_()
  {}

  void Fiber::init(gc<Chunk> chunk)
  {
    ASSERT(callFrames_.count() == 0, "Cannot re-initialize Fiber.");

    call(chunk, 0);
  }

  FiberResult Fiber::run(gc<Object>& result)
  {
    while (true)
    {
      if (Memory::checkCollect()) return FIBER_DID_GC;
      
      CallFrame& frame = callFrames_[-1];
      instruction ins = frame.chunk->code()[frame.ip++];
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
          int slot = GET_B(ins);
          store(frame, slot, frame.chunk->getConstant(index));
          break;
        }
          
        case OP_BUILT_IN:
        {
          int value = GET_A(ins);
          int slot = GET_B(ins);
          store(frame, slot, vm_.getBuiltIn(value));
          break;
        }
          
        case OP_METHOD:
        {
          // Adds a method to a multimethod. A is the index of the multimethod to
          // specialize. B is the index of the method to add.
          int multimethod = GET_A(ins);
          int method = GET_B(ins);
          vm_.defineMethod(multimethod, method);
          break;
        }
          
        case OP_RECORD:
        {
          int firstSlot = GET_A(ins);
          gc<RecordType> type = vm_.getRecordType(GET_B(ins));
          gc<Object> record = RecordObject::create(type, stack_, firstSlot);
          store(frame, GET_C(ins), record);
          break;
        }
          
        case OP_LIST:
        {
          int firstSlot = GET_A(ins);
          int numElements = GET_B(ins);
          
          gc<ListObject> list = new ListObject(numElements);
          for (int i = 0; i < numElements; i++)
          {
            gc<Object> element = load(frame, firstSlot + i);
            list->elements().add(element);
          }
          store(frame, GET_C(ins), list);
          break;
        }
          
        case OP_GET_FIELD:
        {
          bool success = false;
          RecordObject* record = load(frame, GET_A(ins))->toRecord();
          
          // We can't pull record fields out of something that isn't a record.
          // TODO(bob): Should you be able to destructure arbitrary objects by
          // invoking getters with the right name?
          if (record != NULL)
          {
            int symbol = GET_B(ins);
            gc<Object> field = record->getField(symbol);

            // If the record has the field, store it.
            if (!field.isNull())
            {
              store(frame, GET_C(ins), field);
              success = true;
            }
          }
          
          if (!success)
          {
            gc<Object> error = new DynamicObject(vm_.noMatchErrorClass());
            if (!throwError(error)) return FIBER_UNCAUGHT_ERROR;
          }
          break;
        }
          
        case OP_TEST_FIELD:
        {
          bool success = false;
          RecordObject* record = load(frame, GET_A(ins))->toRecord();
          
          // The next instruction is a pseudo-instruction containing the offset
          // to jump to.
          instruction jump = frame.chunk->code()[frame.ip++];
          ASSERT(GET_OP(jump) == OP_JUMP,
                 "Pseudo-instruction after OP_TEST_FIELD must be OP_JUMP.");
          
          // We can't pull record fields out of something that isn't a record.
          // TODO(bob): Should you be able to destructure arbitrary objects by
          // invoking getters with the right name?
          if (record != NULL)
          {
            int symbol = GET_B(ins);
            gc<Object> field = record->getField(symbol);
            
            // If the record has the field, store it.
            if (!field.isNull())
            {
              store(frame, GET_C(ins), field);
              success = true;
            }
          }
          
          // Jump if the match failed.
          if (!success)
          {
            int offset = GET_A(jump);
            frame.ip += offset;
          }
          break;
        }
          
        case OP_GET_VAR:
        {
          int moduleIndex = GET_A(ins);
          int variableIndex = GET_B(ins);
          Module* module = vm_.getModule(moduleIndex);
          gc<Object> object = module->getVariable(variableIndex);
          
          // TODO(bob): Throw UndefinedVariableError.
          if (object.isNull() && !throwError(vm_.getBool(false)))
          {
            return FIBER_UNCAUGHT_ERROR;
          }
          
          store(frame, GET_C(ins), object);
          break;
        }
          
        case OP_SET_VAR:
        {
          int moduleIndex = GET_A(ins);
          int variableIndex = GET_B(ins);
          Module* module = vm_.getModule(moduleIndex);
          gc<Object> value = load(frame, GET_C(ins));
          module->setVariable(variableIndex, value);
          break;
        }
          
        case OP_EQUAL:
        {
          gc<Object> a = loadSlotOrConstant(frame, GET_A(ins));
          gc<Object> b = loadSlotOrConstant(frame, GET_B(ins));
          
          // See if the objects are equal. If they have the same identity, they
          // must be.
          bool equal;
          if (a.sameAs(b))
          {
            equal = true;
          }
          else if (a->type() != b->type())
          {
            // Different types, so not equal.
            equal = false;
          }
          else
          {
            // Same type, so compare values.
            switch (a->type())
            {
              case OBJECT_BOOL:
                equal = a->toBool() == b->toBool();
                break;
                
              case OBJECT_CLASS:
                equal = false;
                break;
                
              case OBJECT_DYNAMIC:
                ASSERT(false, "Equality on arbitrary objects not implemented.");
                break;
                
              case OBJECT_LIST:
                ASSERT(false, "Equality on lists not implemented.");
                break;
                
              case OBJECT_NOTHING:
                ASSERT(false, "Should only be one instance of nothing.");
                break;
                
              case OBJECT_NUMBER:
                equal = a->toNumber() == b->toNumber();
                break;
                
              case OBJECT_RECORD:
                ASSERT(false, "Equality on records not implemented.");
                break;
                
              case OBJECT_STRING:
                equal = a->toString() == b->toString();
                break;
            }
          }

          store(frame, GET_C(ins), vm_.getBool(equal));
          break;
        }
          
        case OP_NOT:
        {
          gc<Object> value = loadSlotOrConstant(frame, GET_A(ins));
          
          // TODO(bob): Handle user-defined types.
          bool result = !value->toBool();
          store(frame, GET_A(ins), vm_.getBool(result));
          break;
        }
          
        case OP_IS:
        {
          gc<Object> value = load(frame, GET_A(ins));
          
          // TODO(bob): Handle it not being a class.
          const ClassObject* expected = load(frame, GET_B(ins))->toClass();
          
          gc<Object> type;
          switch (value->type())
          {
            case OBJECT_BOOL:    type = vm_.boolClass(); break;
            case OBJECT_CLASS:   type = vm_.classClass(); break;
            case OBJECT_DYNAMIC:
            {
              DynamicObject* object = value->toDynamic();
              type = object->classObj();
              break;
            }
            case OBJECT_LIST:    type = vm_.listClass(); break;
            case OBJECT_NOTHING: type = vm_.nothingClass(); break;
            case OBJECT_NUMBER:  type = vm_.numberClass(); break;
            case OBJECT_RECORD:  type = vm_.recordClass(); break;
            case OBJECT_STRING:  type = vm_.stringClass(); break;
          }
          
          store(frame, GET_C(ins), vm_.getBool(type->toClass()->is(*expected)));
          break;
        }
          
        case OP_JUMP:
        {
          int forward = GET_A(ins);
          int offset = GET_B(ins);
          frame.ip += (forward == 1) ? offset : -offset;
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
          
        case OP_CALL:
        {
          gc<Chunk> method = vm_.getMultimethod(GET_A(ins));
          int firstArg = GET_B(ins);
          int stackStart = frame.stackStart + firstArg;
          call(method, stackStart);
          break;
        }
          
        case OP_NATIVE:
        {
          Native native = vm_.getNative(GET_A(ins));
          ArrayView<gc<Object> > args(stack_, frame.stackStart);
          gc<Object> result = native(vm_, args);
          store(frame, GET_B(ins), result);
          break;
        }
          
        case OP_RETURN:
        {
          gc<Object> value = loadSlotOrConstant(frame, GET_A(ins));
          callFrames_.removeAt(-1);
          
          // Discard any try blocks enclosed in the current chunk.
          while (!nearestCatch_.isNull() &&
                 (nearestCatch_->callFrame() >= callFrames_.count()))
          {
            nearestCatch_ = nearestCatch_->parent();
          }
          
          if (callFrames_.count() > 0)
          {
            // Give the result back and resume the calling chunk.
            CallFrame& caller = callFrames_[-1];
            instruction callInstruction = caller.chunk->code()[caller.ip - 1];
            ASSERT(GET_OP(callInstruction) == OP_CALL,
                   "Should be returning to a call.");
            
            store(caller, GET_C(callInstruction), value);
          }
          else
          {
            // The last chunk has returned, so end the fiber.
            result = value;
            return FIBER_DONE;
          }
          break;
        }
          
        case OP_THROW:
        {
          gc<Object> error = load(frame, GET_A(ins));
          if (!throwError(error)) return FIBER_UNCAUGHT_ERROR;
          break;
        }
          
        case OP_ENTER_TRY:
        {
          int offset = frame.ip + GET_A(ins);
          nearestCatch_ = new CatchFrame(nearestCatch_, callFrames_.count() - 1,
                                          offset);
          break;
        }
          
        case OP_EXIT_TRY:
        {
          nearestCatch_ = nearestCatch_->parent();
          break;
        }
          
        case OP_TEST_MATCH:
        {
          gc<Object> pass = load(frame, GET_A(ins));
          if (!pass->toBool())
          {
            gc<Object> error = new DynamicObject(vm_.noMatchErrorClass());
            if (!throwError(error)) return FIBER_UNCAUGHT_ERROR;
          }
          break;
        }
      
        default:
          ASSERT(false, "Unknown opcode.");
          break;
      }
    }
    
    ASSERT(false, "Should not get here.");
    return FIBER_DONE;
  }
  
  void Fiber::reach()
  {
    // Walk the stack.
    CallFrame& frame = callFrames_[-1];
    int numSlots = frame.stackStart + frame.chunk->numSlots();
    
    // Only reach slots that are still in use. We don't shrink the stack, so it
    // may have dead slots at the end that are safe to collect.
    int i;
    for (i = 0; i < numSlots; i++)
    {
      stack_[i].reach();
    }

    // For the remaining slot, clear them out now. When a new call is pushed
    // onto the stack, we allocate slots for it, but we don't clear them out.
    // This means that when a collection occurs, there may be a few slots on
    // the end of the stack that are stale: they are set to whatever they were
    // on some previous call. Since a collection may have occurred between now
    // and then, and dead slots aren't reached (see above), we may have bad
    // pointers. This clears those out so we don't get into that situation. We
    // do it here instead of in call() because call() needs to be as fast as
    // possible.
    for (; i < stack_.count(); i++)
    {
      stack_[i] = gc<Object>();
    }
    
    for (int i = 0; i < callFrames_.count(); i++)
    {
      callFrames_[i].chunk.reach();
    }
  }
  
  void Fiber::call(gc<Chunk> chunk, int stackStart)
  {
    // Allocate slots for the method.
    stack_.grow(stackStart + chunk->numSlots());    
    callFrames_.add(CallFrame(chunk, stackStart));
  }

  bool Fiber::throwError(gc<Object> error)
  {
    // If there is nothing to catch it, end the fiber.
    if (nearestCatch_.isNull()) return false;
    
    // Unwind any nested callframes above the one containing the catch
    // clause.
    callFrames_.truncate(nearestCatch_->callFrame() + 1);
    
    // Jump to the catch handler.
    CallFrame& frame = callFrames_[-1];
    frame.ip = nearestCatch_->offset();
    
    // The next instruction is a pseudo-op identifying where the error is.
    instruction errorIns = frame.chunk->code()[frame.ip];
    ASSERT(GET_OP(errorIns) == OP_MOVE,
        "Expect pseudo-instruction at beginning of catch code.");
    int errorSlot = GET_A(errorIns);
    store(frame, errorSlot, error);
    frame.ip++;
    
    // Discard the try block now that we are outside of it.
    nearestCatch_ = nearestCatch_->parent();
    
    return true;
  }
  
  gc<Object> Fiber::loadSlotOrConstant(const CallFrame& frame, int index)
  {
    if (IS_CONSTANT(index))
    {
      return frame.chunk->getConstant(GET_CONSTANT(index));
    }
    else
    {
      return load(frame, index);
    }
  }
  
  void CatchFrame::reach()
  {
    parent_.reach();
  }
}