#include <sstream>

#include "Object.h"
#include "Natives.h"
#include "VM.h"

namespace magpie
{
  NATIVE(bindCore)
  {
    vm.bindCore();
    return vm.nothing();
  }

  NATIVE(bindIO)
  {
    vm.bindIO();
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
    return vm.getBool(Object::equal(args[0], args[1]));
  }

  NATIVE(objectNotEqualsObject)
  {
    return vm.getBool(!Object::equal(args[0], args[1]));
  }

  NATIVE(printString)
  {
    std::cout << args[0]->toString() << std::endl;
    return args[0];
  }

  NATIVE(stringPlusString)
  {
    return new StringObject(String::concat(args[0]->asString(),
                                           args[1]->asString()));
  }

  NATIVE(intPlusInt)
  {
    return new IntObject(args[0]->asInt() + args[1]->asInt());
  }

  NATIVE(intPlusFloat)
  {
    return new FloatObject(args[0]->asInt() + args[1]->asFloat());
  }

  NATIVE(floatPlusInt)
  {
    return new FloatObject(args[0]->asFloat() + args[1]->asInt());
  }

  NATIVE(floatPlusFloat)
  {
    return new FloatObject(args[0]->asFloat() + args[1]->asFloat());
  }
    
  NATIVE(intMinusInt)
  {
    return new IntObject(args[0]->asInt() - args[1]->asInt());
  }

  NATIVE(intMinusFloat)
  {
    return new FloatObject(args[0]->asInt() - args[1]->asFloat());
  }

  NATIVE(floatMinusInt)
  {
    return new FloatObject(args[0]->asFloat() - args[1]->asInt());
  }

  NATIVE(floatMinusFloat)
  {
    return new FloatObject(args[0]->asFloat() - args[1]->asFloat());
  }

  NATIVE(intTimesInt)
  {
    return new IntObject(args[0]->asInt() * args[1]->asInt());
  }

  NATIVE(intTimesFloat)
  {
    return new FloatObject(args[0]->asInt() * args[1]->asFloat());
  }

  NATIVE(floatTimesInt)
  {
    return new FloatObject(args[0]->asFloat() * args[1]->asInt());
  }

  NATIVE(floatTimesFloat)
  {
    return new FloatObject(args[0]->asFloat() * args[1]->asFloat());
  }

  NATIVE(intDivInt)
  {
    return new IntObject(args[0]->asInt() / args[1]->asInt());
  }

  NATIVE(intDivFloat)
  {
    return new FloatObject(args[0]->asInt() / args[1]->asFloat());
  }

  NATIVE(floatDivInt)
  {
    return new FloatObject(args[0]->asFloat() / args[1]->asInt());
  }

  NATIVE(floatDivFloat)
  {
    return new FloatObject(args[0]->asFloat() / args[1]->asFloat());
  }
  
  NATIVE(intModInt)
  {
    return new IntObject(args[0]->asInt() % args[1]->asInt());
  }

  NATIVE(minusInt)
  {
    return new IntObject(-args[0]->asInt());
  }

  NATIVE(minusFloat)
  {
    return new FloatObject(-args[0]->asFloat());
  }
  
  NATIVE(intCompareToInt)
  {
    int difference = args[0]->asInt() - args[1]->asInt();
    return new IntObject(sgn(difference));
  }
  
  NATIVE(intCompareToFloat)
  {
    double difference = args[0]->asInt() - args[1]->asFloat();
    return new IntObject(sgn(difference));
  }
  
  NATIVE(floatCompareToInt)
  {
    double difference = args[0]->asFloat() - args[1]->asInt();
    return new IntObject(sgn(difference));
  }
  
  NATIVE(floatCompareToFloat)
  {
    double difference = args[0]->asFloat() - args[1]->asFloat();
    return new IntObject(sgn(difference));
  }

  NATIVE(intSgn)
  {
    return new IntObject(sgn(args[0]->asInt()));
  }

  NATIVE(floatSgn)
  {
    return new IntObject(sgn(args[0]->asFloat()));
  }

  NATIVE(stringCount)
  {
    return new IntObject(args[0]->asString()->length());
  }

  NATIVE(stringSubscriptInt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<String> string = args[0]->asString();

    // TODO(bob): Handle non-ASCII.
    char c = (*string)[args[1]->asInt()];

    return new CharacterObject(c);
  }

  NATIVE(floatToString)
  {
    // TODO(bob): Hackish. "%g" gives nice short results without trailing
    // zeroes, but it also drops the "." completely if not needed. We want to
    // show that (i.e. "1.0") so that floats can be distinguished from numbers.
    // So just look for a "." in the result string and add ".0" if not found.
    // Do our own float->string conversion?
    gc<String> string = String::format("%g", args[0]->asFloat());
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
    return new StringObject(String::format("%d", args[0]->asInt()));
  }

  NATIVE(sleepMsInt)
  {
    fiber.sleep(args[0]->asInt());
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(channelClose)
  {
    ChannelObject* channel = args[0]->asChannel();

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
    ChannelObject* channel = args[0]->asChannel();
    return vm.getBool(channel->isOpen());
  }

  NATIVE(channelNew)
  {
    return new ChannelObject();
  }
  
  NATIVE(channelReceive)
  {
    // Hang this fiber off the channel we're waiting for a value from.
    ChannelObject* channel = args[0]->asChannel();
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
    ChannelObject* channel = args[0]->asChannel();

    // Send the value and suspend this fiber until it's been received.
    channel->send(&fiber, args[1]);

    // TODO(bob): If the channel is buffered, sending won't always suspend.
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(fileClose)
  {
    gc<FileObject> fileObj = args[0]->asFile();
    fileObj->close(&fiber);

    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }
  
  NATIVE(fileIsOpen)
  {
    gc<FileObject> fileObj = args[0]->asFile();
    return vm.getBool(fileObj->isOpen());
  }
  
  NATIVE(fileOpen)
  {
    fiber.openFile(args[1]->asString());
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(fileRead)
  {
    /*
    gc<FileObject> fileObj = args[0]->asFile();
    fiber.readFile(fileObj);
     */
    ASSERT(false, "File read native not implemented.");

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
    ListObject* list = args[0]->asList();
    list->elements().add(args[1]);
    return args[1];
  }

  NATIVE(listClear)
  {
    ListObject* list = args[0]->asList();
    list->elements().clear();
    return vm.nothing();
  }
  
  NATIVE(listCount)
  {
    ListObject* list = args[0]->asList();
    return new IntObject(list->elements().count());
  }

  NATIVE(listInsert)
  {
    ListObject* list = args[0]->asList();
    list->elements().insert(args[1], args[2]->asInt());
    return args[1];
  }

  NATIVE(listRemoveAt)
  {
    // Note: bounds checking is handled by core before calling this.
    ListObject* list = args[0]->asList();
    return list->elements().removeAt(args[1]->asInt());
  }
  
  NATIVE(listSubscriptInt)
  {
    // Note: bounds checking is handled by core before calling this.
    ListObject* list = args[0]->asList();
    return list->elements()[args[1]->asInt()];
  }

  NATIVE(listSubscriptRange)
  {
    // Note: bounds checking is handled by core before calling this.
    ListObject* source = args[0]->asList();
    int first = args[1]->asInt();
    int last = args[2]->asInt();

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
    ListObject* list = args[0]->asList();
    list->elements()[args[1]->asInt()] = args[2];
    return args[2];
  }
}

