#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "MagpieString.h"
#include "Object.h"

namespace magpie
{
  void Chunk::setCode(const Array<instruction>& code, int numSlots)
  {
    // TODO(bob): Copying here is lame!
    code_ = code;
    numSlots_ = numSlots;
  }

  int Chunk::addConstant(gc<Object> constant)
  {
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    constants_.add(constant);
    return constants_.count() - 1;
  }
  
  gc<Object> Chunk::getConstant(int index) const
  {
    ASSERT_INDEX(index, constants_.count());
    return constants_[index];
  }

  void Chunk::debugTrace() const
  {
    using namespace std;
    
    // TODO(bob): Constants.
    
    for (int i = 0; i < code_.count(); i++)
    {
      debugTrace(code_[i]);
    }
  }
  
  void Chunk::debugTrace(instruction ins) const
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
        
      case OP_METHOD:
        cout << "METHOD        " << GET_A(ins) << " <- " << GET_B(ins);
        break;
        
      case OP_RECORD:
        cout << "RECORD        " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_LIST:
        cout << "LIST          [" << GET_A(ins) << "..." << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_GET_FIELD:
        cout << "GET_FIELD     " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_TEST_FIELD:
        cout << "TEST_FIELD    " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_GET_VAR:
        cout << "OP_GET_VAR    module " << GET_A(ins) << ", var " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_SET_VAR:
        cout << "OP_SET_VAR    module " << GET_A(ins) << ", var " << GET_B(ins) << " <- " << GET_C(ins);
        break;
        
      case OP_EQUAL:
        cout << "EQUAL         " << GET_A(ins) << " == " << GET_B(ins) << " -> " << GET_C(ins);
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
        
      case OP_NATIVE:
        cout << "NATIVE        " << GET_A(ins) << "(" << GET_B(ins) << ") -> " << GET_C(ins);
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

  void Chunk::reach()
  {
    Memory::reach(constants_);
  }
  
  Multimethod::Multimethod(gc<String> signature)
  : signature_(signature),
    chunk_(),
    methods_()
  {}
  
  void Multimethod::addMethod(gc<Method> method)
  {
    methods_.add(method);
    
    // Clear out the code since it needs to be recompiled.
    chunk_ = NULL;
  }
  
  gc<Chunk> Multimethod::getChunk(VM& vm)
  {
    // Re-compile if methods have been defined since the last time this was
    // called.
    if (chunk_.isNull())
    {
      ErrorReporter reporter;
      chunk_ = Compiler::compileMultimethod(vm, reporter, *this);
    }
    
    return chunk_;
  }
}