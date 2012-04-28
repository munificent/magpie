#include "Method.h"
#include "MagpieString.h"
#include "Object.h"

namespace magpie
{
  temp<Method> Method::create(gc<String> name,
                              const Array<instruction>& code,
                              const Array<gc<Object> >& constants,
                              int numRegisters)
  {
    return Memory::makeTemp(new Method(name, code, constants, numRegisters));
  }

  temp<Method> Method::create(gc<String> name,
                              Primitive primitive)
  {
    return Memory::makeTemp(new Method(name, primitive));
  }
  
  gc<Object> Method::getConstant(int index) const
  {
    ASSERT_INDEX(index, constants_.count());
    return constants_[index];
  }
  
  void Method::debugTrace() const
  {
    using namespace std;
    
    cout << name_ << endl;
    
    // TODO(bob): Constants.
    
    for (int i = 0; i < code_.count(); i++)
    {
      debugTrace(code_[i]);
    }
  }
  
  void Method::debugTrace(instruction ins) const
  {
    using namespace std;
    
    switch (GET_OP(ins))
    {
      case OP_MOVE:
        cout << "MOVE          " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_CONSTANT:
        cout << "CONSTANT      " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_BOOL:
        cout << "BOOL          " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_ADD:
        cout << "ADD           " << GET_A(ins) << " + " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_SUBTRACT:
        cout << "SUBTRACT      " << GET_A(ins) << " - " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_MULTIPLY:
        cout << "MULTIPLY      " << GET_A(ins) << " - " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_DIVIDE:
        cout << "DIVIDE        " << GET_A(ins) << " - " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_LESS_THAN:
        cout << "LESS_THAN     " << GET_A(ins) << " < " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_JUMP:
        cout << "JUMP          " << GET_A(ins);
        break;
        
      case OP_JUMP_IF_FALSE:
        cout << "JUMP_IF_FALSE " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_CALL:
        cout << "CALL          " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_END:
        cout << "END           " << GET_A(ins);
        break;
    }
    
    cout << endl;
  }

  void Method::reach()
  {
    Memory::reach(name_);
    Memory::reach(constants_);
  }
  
  void MethodScope::declare(gc<String> name)
  {
    names_.add(name);
    methods_.add(gc<Method>());
  }
  
  void MethodScope::define(gc<String> name, gc<Method> method)
  {
    int index = find(name);
    methods_[index] = method;
  }
  
  void MethodScope::define(gc<String> name, Primitive primitive)
  {
    names_.add(name);
    methods_.add(Method::create(name, primitive));
  }
  
  int MethodScope::find(gc<String> name) const
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      if (names_[i] == name) return i;
    }
    
    return -1;
  }
  
  gc<Method> MethodScope::findMain() const
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      if (*names_[i] == "main") return methods_[i];
    }
    
    return gc<Method>();
  }
  
  void MethodScope::reach()
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      Memory::reach(methods_[i]);
      Memory::reach(names_[i]);
    }
  }
}