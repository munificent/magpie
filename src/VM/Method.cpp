#include "Method.h"
#include "MagpieString.h"
#include "Object.h"

namespace magpie
{
  void Method::setCode(const Array<instruction>& code, int numSlots)
  {
    // TODO(bob): Copying here is lame!
    code_ = code;
    numSlots_ = numSlots;
  }

  int Method::addConstant(gc<Object> constant)
  {
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    constants_.add(constant);
    return constants_.count() - 1;
  }
  
  gc<Object> Method::getConstant(int index) const
  {
    ASSERT_INDEX(index, constants_.count());
    return constants_[index];
  }

  void Method::debugTrace() const
  {
    using namespace std;
    
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
        
      case OP_BUILT_IN:
        cout << "BUILT_IN      " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_RECORD:
        cout << "RECORD        " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_GET_FIELD:
        cout << "GET_FIELD     " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_TEST_FIELD:
        cout << "TEST_FIELD    " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_GET_MODULE:
        cout << "GET_MODULE    import " << GET_A(ins) << ", var " << GET_B(ins) << " -> " << GET_C(ins);
        break;

      case OP_EQUAL:
        cout << "EQUAL         " << GET_A(ins) << " == " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_LESS_THAN:
        cout << "LESS_THAN     " << GET_A(ins) << " < " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_GREATER_THAN:
        cout << "GREATER_THAN  " << GET_A(ins) << " > " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_NOT:
        cout << "NOT           " << GET_A(ins);
        break;
        
      case OP_IS:
        cout << "IS            " << GET_A(ins) << " is " << GET_B(ins);
        break;
        
      case OP_JUMP:
        cout << "JUMP          " << GET_A(ins) << " " << GET_B(ins);
        break;
        
      case OP_JUMP_IF_FALSE:
        cout << "JUMP_IF_FALSE " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_JUMP_IF_TRUE:
        cout << "JUMP_IF_TRUE " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_CALL:
        cout << "CALL          " << GET_A(ins) << "(" << GET_B(ins) << ") -> " << GET_C(ins);
        break;
        
      case OP_RETURN:
        cout << "RETURN        " << GET_A(ins);
        break;
        
      case OP_THROW:
        cout << "THROW         " << GET_A(ins);
        break;
        
      case OP_ENTER_TRY:
        cout << "ENTER_TRY     " << GET_A(ins);
        break;
        
      case OP_EXIT_TRY:
        cout << "EXIT_TRY      ";
        break;
        
      case OP_TEST_MATCH:
        cout << "TEST_MATCH    " << GET_A(ins);
        break;
    }
    
    cout << endl;
  }

  void Method::reach()
  {
    Memory::reach(constants_);
  }
  
  void MethodScope::define(gc<String> name, gc<Method> method)
  {
    names_.add(name);
    methods_.add(method);
  }
  
  void MethodScope::define(gc<String> name, Primitive primitive)
  {
    names_.add(name);
    methods_.add(new Method(primitive));
  }
  
  int MethodScope::find(gc<String> name) const
  {
    for (int i = 0; i < methods_.count(); i++)
    {
      if (names_[i] == name) return i;
    }
    
    return -1;
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