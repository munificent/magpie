#include <sstream>

#include "Object.h"
#include "Natives.h"
#include "VM.h"

namespace magpie
{
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

  NATIVE(printString)
  {
    std::cout << args[0]->toString() << std::endl;
    return args[0];
  }

  NATIVE(numPlusNum)
  {
    double c = args[0]->asNumber() + args[1]->asNumber();
    return new NumberObject(c);
  }
  
  NATIVE(stringPlusString)
  {
    return new StringObject(
        String::concat(args[0]->asString(), args[1]->asString()));
  }
  
  NATIVE(numMinusNum)
  {
    double c = args[0]->asNumber() - args[1]->asNumber();
    return new NumberObject(c);
  }
  
  NATIVE(numTimesNum)
  {
    double c = args[0]->asNumber() * args[1]->asNumber();
    return new NumberObject(c);
  }
  
  NATIVE(numDivNum)
  {
    double c = args[0]->asNumber() / args[1]->asNumber();
    return new NumberObject(c);
  }
  
  NATIVE(numLessThanNum)
  {
    return vm.getBool(args[0]->asNumber() < args[1]->asNumber());
  }
  
  NATIVE(numLessThanEqualToNum)
  {
    return vm.getBool(args[0]->asNumber() <= args[1]->asNumber());
  }
  
  NATIVE(numGreaterThanNum)
  {
    return vm.getBool(args[0]->asNumber() > args[1]->asNumber());
  }
  
  NATIVE(numGreaterThanEqualToNum)
  {
    return vm.getBool(args[0]->asNumber() >= args[1]->asNumber());
  }
  
  NATIVE(stringCount)
  {
    double c = args[0]->asString()->length();
    return new NumberObject(c);
  }
  
  NATIVE(numToString)
  {
    double n = args[0]->asNumber();
    return new StringObject(String::format("%g", n));
  }

  NATIVE(functionCall)
  {
    result = NATIVE_RESULT_CALL;
    return args[0];
  }

  NATIVE(functionRun)
  {
    FunctionObject* function = args[0]->asFunction();

    gc<Fiber> functionFiber = new Fiber(vm, function->chunk());
    vm.addFiber(functionFiber);

    // TODO(bob): What should this return?
    return args[0];
  }
  
  NATIVE(listAdd)
  {
    ListObject* list = args[0]->asList();
    list->elements().add(args[1]);
    return args[1];
  }
  
  NATIVE(listCount)
  {
    ListObject* list = args[0]->asList();
    return new NumberObject(list->elements().count());
  }
  
  NATIVE(listIndex)
  {
    ListObject* list = args[0]->asList();
    // TODO(bob): What if the index isn't an int?
    return list->elements()[static_cast<int>(args[1]->asNumber())];
  }
  
  NATIVE(listIndexSet)
  {
    ListObject* list = args[0]->asList();
    // TODO(bob): What if the index isn't an int?
    list->elements()[static_cast<int>(args[1]->asNumber())] = args[2];
    return args[2];
  }

  NATIVE(listInsert)
  {
    ListObject* list = args[0]->asList();
    gc<Object> value = args[1];
    // TODO(bob): How do we want to handle non-integer indices?
    int index = static_cast<int>(args[2]->asNumber());
    list->elements().insert(value, index);
    return args[1];
  }
}

