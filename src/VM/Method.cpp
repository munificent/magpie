#include "Method.h"
#include "MagpieString.h"
#include "Object.h"

namespace magpie
{
  void Method::setCode(const Array<instruction>& code, int maxRegisters)
  {
    // TODO(bob): Copying here is lame!
    code_ = code;
    numRegisters_ = maxRegisters;
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
  
  int Method::addMethod(gc<Method> method)
  {
    methods_.add(method);
    return methods_.count() - 1;
  }

  gc<Method> Method::getMethod(int index) const
  {
    ASSERT_INDEX(index, methods_.count());
    return methods_[index];
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
        
      case OP_DEF_METHOD:
        cout << "DEF_METHOD    " << GET_A(ins) << " " << GET_B(ins);
        break;
        
      case OP_GET_FIELD:
        cout << "GET_FIELD     " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_GET_MODULE:
        cout << "GET_MODULE    import " << GET_A(ins) << ", var " << GET_B(ins) << " -> " << GET_C(ins);
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
        cout << "JUMP          " << GET_A(ins);
        break;
        
      case OP_JUMP_IF_FALSE:
        cout << "JUMP_IF_FALSE " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_JUMP_IF_TRUE:
        cout << "JUMP_IF_TRUE " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_CALL:
        cout << "CALL          " << GET_A(ins) << " -> " << GET_B(ins);
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
    Memory::reach(methods_);
  }
  
  int MethodScope::declare(gc<String> name)
  {
    // If a method is already declared with that name (either as a forward
    // declaration, or as an actual previous method) then reuse that index.
    int existing = find(name);
    if (existing != -1) return existing;
    
    names_.add(name);
    methods_.add(gc<Method>());
    return names_.count() - 1;
  }
  
  void MethodScope::define(int index, gc<Method> method)
  {
    ASSERT(methods_[index].isNull(),
           "Multimethods are't implemented yet, so cannot redefine an "
           "already defined method.");
    
    methods_[index] = method;
  }

  void MethodScope::define(gc<String> name, gc<Method> method)
  {
    define(find(name), method);
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