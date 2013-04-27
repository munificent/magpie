#include <sstream>

#include "Object.h"
#include "Native/Core.h"
#include "VM.h"

namespace magpie
{
  // TODO(bob): Move this to be closer to other Task classes.
  class PrintTask : public Task
  {
    friend class TaskList;

  public:
    PrintTask(gc<Fiber> fiber, gc<Object> value, int numBuffers);
    virtual void kill();
    virtual void reach();

    uv_stream_t* stream()
    {
      return reinterpret_cast<uv_stream_t*>(fiber()->scheduler().tty());
    }

    gc<Object> value() { return value_; }
    
  private:
    uv_write_t   request_;
    uv_stream_t* stream_;
    uv_buf_t     buffers_[2];
    gc<Object>   value_;
  };

  void printCallback(uv_write_t* req, int status)
  {
    // TODO(bob): Check status.
    PrintTask* task = (PrintTask*) req->data;
    task->complete(task->value());
  }

  PrintTask::PrintTask(gc<Fiber> fiber, gc<Object> value, int numBuffers)
  : Task(fiber),
    value_(value)
  {
    request_.data = this;

    buffers_[0].base = const_cast<char*>(asString(value)->cString());
    buffers_[0].len = asString(value)->length();

    buffers_[1].base = const_cast<char*>("\n");
    buffers_[1].len = 1;

    uv_write(&request_,
             reinterpret_cast<uv_stream_t*>(fiber->scheduler().tty()),
             buffers_, 2, printCallback);
  }

  void PrintTask::kill()
  {
    // TODO(bob): Do nothing? What about the request?
  }

  void PrintTask::reach()
  {
    Task::reach();
    value_.reach();
  }
  
  NATIVE(bindCore)
  {
    vm.bindClass("core", CLASS_BOOL, "Bool");
    vm.bindClass("core", CLASS_CHANNEL, "Channel");
    vm.bindClass("core", CLASS_CHAR, "Char");
    vm.bindClass("core", CLASS_CLASS, "Class");
    vm.bindClass("core", CLASS_DONE, "Done");
    vm.bindClass("core", CLASS_FUNCTION, "Function");
    vm.bindClass("core", CLASS_FLOAT, "Float");
    vm.bindClass("core", CLASS_INT, "Int");
    vm.bindClass("core", CLASS_LIST, "List");
    vm.bindClass("core", CLASS_NOTHING, "Nothing");
    vm.bindClass("core", CLASS_RECORD, "Record");
    vm.bindClass("core", CLASS_STRING, "String");
    vm.bindClass("core", CLASS_NO_MATCH_ERROR, "NoMatchError");
    vm.bindClass("core", CLASS_NO_METHOD_ERROR, "NoMethodError");
    vm.bindClass("core", CLASS_UNDEFINED_VAR_ERROR, "UndefinedVarError");
    return vm.nothing();
  }

  NATIVE(objectClass)
  {
    return args[0]->getClass(vm);
  }

  NATIVE(objectNew)
  {
    // This assumes the args list has, in order, the class object and then all
    // of the field values for that class.
    return DynamicObject::create(args);
  }

  NATIVE(objectToString)
  {
    return new StringObject(args[0]->toString());
  }

  NATIVE(objectEqualsObject)
  {
    return vm.getBool(args[0]->equals(args[1]));
  }

  NATIVE(objectNotEqualsObject)
  {
    return vm.getBool(!args[0]->equals(args[1]));
  }
  
  NATIVE(printString)
  {
    new PrintTask(&fiber, args[0], 2);

    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(printErrorString)
  {
    // TODO(bob): Go through libuv for this?
    std::cerr << args[0] << std::endl;
    return args[0];
  }

  NATIVE(stringPlusString)
  {
    return new StringObject(String::concat(asString(args[0]),
                                           asString(args[1])));
  }

  NATIVE(intPlusInt)
  {
    return new IntObject(asInt(args[0]) + asInt(args[1]));
  }

  NATIVE(intPlusFloat)
  {
    return new FloatObject(asInt(args[0]) + asFloat(args[1]));
  }

  NATIVE(floatPlusInt)
  {
    return new FloatObject(asFloat(args[0]) + asInt(args[1]));
  }

  NATIVE(floatPlusFloat)
  {
    return new FloatObject(asFloat(args[0]) + asFloat(args[1]));
  }

  NATIVE(intMinusInt)
  {
    return new IntObject(asInt(args[0]) - asInt(args[1]));
  }

  NATIVE(intMinusFloat)
  {
    return new FloatObject(asInt(args[0]) - asFloat(args[1]));
  }

  NATIVE(floatMinusInt)
  {
    return new FloatObject(asFloat(args[0]) - asInt(args[1]));
  }

  NATIVE(floatMinusFloat)
  {
    return new FloatObject(asFloat(args[0]) - asFloat(args[1]));
  }

  NATIVE(intTimesInt)
  {
    return new IntObject(asInt(args[0]) * asInt(args[1]));
  }

  NATIVE(intTimesFloat)
  {
    return new FloatObject(asInt(args[0]) * asFloat(args[1]));
  }

  NATIVE(floatTimesInt)
  {
    return new FloatObject(asFloat(args[0]) * asInt(args[1]));
  }

  NATIVE(floatTimesFloat)
  {
    return new FloatObject(asFloat(args[0]) * asFloat(args[1]));
  }

  NATIVE(intDivInt)
  {
    return new IntObject(asInt(args[0]) / asInt(args[1]));
  }

  NATIVE(intDivFloat)
  {
    return new FloatObject(asInt(args[0]) / asFloat(args[1]));
  }

  NATIVE(floatDivInt)
  {
    return new FloatObject(asFloat(args[0]) / asInt(args[1]));
  }

  NATIVE(floatDivFloat)
  {
    return new FloatObject(asFloat(args[0]) / asFloat(args[1]));
  }

  NATIVE(intModInt)
  {
    return new IntObject(asInt(args[0]) % asInt(args[1]));
  }

  NATIVE(minusInt)
  {
    return new IntObject(-asInt(args[0]));
  }

  NATIVE(minusFloat)
  {
    return new FloatObject(-asFloat(args[0]));
  }

  NATIVE(intCompareToInt)
  {
    int difference = asInt(args[0]) - asInt(args[1]);
    return new IntObject(sgn(difference));
  }

  NATIVE(intCompareToFloat)
  {
    double difference = asInt(args[0]) - asFloat(args[1]);
    return new IntObject(sgn(difference));
  }

  NATIVE(floatCompareToInt)
  {
    double difference = asFloat(args[0]) - asInt(args[1]);
    return new IntObject(sgn(difference));
  }

  NATIVE(floatCompareToFloat)
  {
    double difference = asFloat(args[0]) - asFloat(args[1]);
    return new IntObject(sgn(difference));
  }

  NATIVE(intSgn)
  {
    return new IntObject(sgn(asInt(args[0])));
  }

  NATIVE(floatSgn)
  {
    return new IntObject(sgn(asFloat(args[0])));
  }

  NATIVE(stringCount)
  {
    return new IntObject(asString(args[0])->length());
  }

  NATIVE(stringSubscriptInt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<String> string = asString(args[0]);

    // TODO(bob): Handle non-ASCII.
    char c = (*string)[asInt(args[1])];

    return new CharacterObject(c);
  }

  NATIVE(floatToString)
  {
    // TODO(bob): Hackish. "%g" gives nice short results without trailing
    // zeroes, but it also drops the "." completely if not needed. We want to
    // show that (i.e. "1.0") so that floats can be distinguished from numbers.
    // So just look for a "." in the result string and add ".0" if not found.
    // Do our own float->string conversion?
    gc<String> string = String::format("%g", asFloat(args[0]));
    bool hasDecimal = false;
    for (int i = 0; i < string->length(); i++)
    {
      if ((*string)[i] == '.')
      {
        hasDecimal = true;
        break;
      }
    }

    if (!hasDecimal)
    {
      string = String::format("%s.0", string->cString());
    }

    return new StringObject(string);
  }

  NATIVE(intToString)
  {
    return new StringObject(String::format("%d", asInt(args[0])));
  }

  NATIVE(sleepMsInt)
  {
    fiber.sleep(asInt(args[0]));
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(channelClose)
  {
    gc<ChannelObject> channel = asChannel(args[0]);

    if (channel->close(vm, &fiber))
    {
      result = NATIVE_RESULT_SUSPEND;
      return NULL;
    }
    else
    {
      return vm.nothing();
    }
  }

  NATIVE(channelIsOpen)
  {
    gc<ChannelObject> channel = asChannel(args[0]);
    return vm.getBool(channel->isOpen());
  }

  NATIVE(channelNew)
  {
    return new ChannelObject();
  }

  NATIVE(channelReceive)
  {
    // Hang this fiber off the channel we're waiting for a value from.
    gc<ChannelObject> channel = asChannel(args[0]);
    gc<Object> value = channel->receive(vm, &fiber);

    // If we don't have an immediate value, suspend this fiber.
    if (value.isNull())
    {
      result = NATIVE_RESULT_SUSPEND;
    }

    return value;
  }

  NATIVE(channelSend)
  {
    gc<ChannelObject> channel = asChannel(args[0]);

    // Send the value and suspend this fiber until it's been received.
    channel->send(&fiber, args[1]);

    // TODO(bob): If the channel is buffered, sending won't always suspend.
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(functionCall)
  {
    result = NATIVE_RESULT_CALL;
    return args[0];
  }

  NATIVE(listAdd)
  {
    gc<ListObject> list = asList(args[0]);
    list->elements().add(args[1]);
    return args[1];
  }

  NATIVE(listClear)
  {
    gc<ListObject> list = asList(args[0]);
    list->elements().clear();
    return vm.nothing();
  }

  NATIVE(listCount)
  {
    gc<ListObject> list = asList(args[0]);
    return new IntObject(list->elements().count());
  }

  NATIVE(listInsert)
  {
    gc<ListObject> list = asList(args[0]);
    list->elements().insert(args[1], asInt(args[2]));
    return args[1];
  }

  NATIVE(listRemoveAt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<ListObject> list = asList(args[0]);
    return list->elements().removeAt(asInt(args[1]));
  }

  NATIVE(listSubscriptInt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<ListObject> list = asList(args[0]);
    return list->elements()[asInt(args[1])];
  }

  NATIVE(listSubscriptRange)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<ListObject> source = asList(args[0]);
    int first = asInt(args[1]);
    int last = asInt(args[2]);

    int size = last - first;
    gc<ListObject> list = new ListObject(size);
    for (int i = 0; i < size; i++)
    {
      list->elements().add(source->elements()[i + first]);
    }

    return list;
  }

  NATIVE(listSubscriptSetInt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<ListObject> list = asList(args[0]);
    list->elements()[asInt(args[1])] = args[2];
    return args[2];
  }

  NATIVE(exit)
  {
    exit(asInt(args[0]));
    return vm.nothing();
  }

  void defineCoreNatives(VM& vm)
  {
    DEF_NATIVE(bindCore);
    DEF_NATIVE(objectClass);
    DEF_NATIVE(objectNew);
    DEF_NATIVE(objectToString);
    DEF_NATIVE(objectEqualsObject);
    DEF_NATIVE(objectNotEqualsObject);
    DEF_NATIVE(printString);
    DEF_NATIVE(printErrorString);
    DEF_NATIVE(intPlusInt);
    DEF_NATIVE(intPlusFloat);
    DEF_NATIVE(floatPlusInt);
    DEF_NATIVE(floatPlusFloat);
    DEF_NATIVE(stringPlusString);
    DEF_NATIVE(intMinusInt);
    DEF_NATIVE(intMinusFloat);
    DEF_NATIVE(floatMinusInt);
    DEF_NATIVE(floatMinusFloat);
    DEF_NATIVE(intTimesInt);
    DEF_NATIVE(intTimesFloat);
    DEF_NATIVE(floatTimesInt);
    DEF_NATIVE(floatTimesFloat);
    DEF_NATIVE(intDivInt);
    DEF_NATIVE(intDivFloat);
    DEF_NATIVE(floatDivInt);
    DEF_NATIVE(floatDivFloat);
    DEF_NATIVE(intModInt);
    DEF_NATIVE(minusInt);
    DEF_NATIVE(minusFloat);
    DEF_NATIVE(intCompareToInt);
    DEF_NATIVE(intCompareToFloat);
    DEF_NATIVE(floatCompareToInt);
    DEF_NATIVE(floatCompareToFloat);
    DEF_NATIVE(intSgn);
    DEF_NATIVE(floatSgn);
    DEF_NATIVE(stringCount);
    DEF_NATIVE(stringSubscriptInt);
    DEF_NATIVE(floatToString);
    DEF_NATIVE(intToString);
    DEF_NATIVE(sleepMsInt);
    DEF_NATIVE(channelClose);
    DEF_NATIVE(channelIsOpen);
    DEF_NATIVE(channelNew);
    DEF_NATIVE(channelReceive);
    DEF_NATIVE(channelSend);
    DEF_NATIVE(functionCall);
    DEF_NATIVE(listAdd);
    DEF_NATIVE(listClear);
    DEF_NATIVE(listCount);
    DEF_NATIVE(listInsert);
    DEF_NATIVE(listRemoveAt);
    DEF_NATIVE(listSubscriptInt);
    DEF_NATIVE(listSubscriptRange);
    DEF_NATIVE(listSubscriptSetInt);
    DEF_NATIVE(exit);
  }
}

