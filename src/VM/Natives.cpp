#include <sstream>

#include "Object.h"
#include "Natives.h"
#include "VM.h"

namespace magpie
{
  NATIVE(objectNew)
  {
    // This assumes the args list has, in order, the class object and then all
    // of the field values for that class.
    return DynamicObject::create(args);
  }

  NATIVE(objectToString)
  {
    // TODO(bob): Can definitely do something more efficient here!
    std::stringstream stream;
    stream << args[0];
    return new StringObject(String::create(stream.str().c_str()));
  }

  NATIVE(printString)
  {
    std::cout << args[0]->toString() << std::endl;
    return args[0];
  }

  NATIVE(numPlusNum)
  {
    double c = args[0]->toNumber() + args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(stringPlusString)
  {
    return new StringObject(
        String::concat(args[0]->toString(), args[1]->toString()));
  }
  
  NATIVE(numMinusNum)
  {
    double c = args[0]->toNumber() - args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(numTimesNum)
  {
    double c = args[0]->toNumber() * args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(numDivNum)
  {
    double c = args[0]->toNumber() / args[1]->toNumber();
    return new NumberObject(c);
  }
  
  NATIVE(numLessThanNum)
  {
    return vm.getBool(args[0]->toNumber() < args[1]->toNumber());
  }
  
  NATIVE(numLessThanEqualToNum)
  {
    return vm.getBool(args[0]->toNumber() <= args[1]->toNumber());
  }
  
  NATIVE(numGreaterThanNum)
  {
    return vm.getBool(args[0]->toNumber() > args[1]->toNumber());
  }
  
  NATIVE(numGreaterThanEqualToNum)
  {
    return vm.getBool(args[0]->toNumber() >= args[1]->toNumber());
  }
  
  NATIVE(stringCount)
  {
    double c = args[0]->toString()->length();
    return new NumberObject(c);
  }
  
  NATIVE(numToString)
  {
    double n = args[0]->toNumber();
    return new StringObject(String::format("%g", n));
  }
  
  NATIVE(listCount)
  {
    ListObject* list = args[0]->toList();
    return new NumberObject(list->elements().count());
  }
  
  NATIVE(listIndex)
  {
    ListObject* list = args[0]->toList();
    // TODO(bob): What if the index isn't an int?
    return list->elements()[static_cast<int>(args[1]->toNumber())];
  }
  
  NATIVE(listIndexSet)
  {
    ListObject* list = args[0]->toList();
    // TODO(bob): What if the index isn't an int?
    list->elements()[static_cast<int>(args[1]->toNumber())] = args[2];
    return args[2];
  }
}

