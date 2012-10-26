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
        cout << "MOVE            " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_CONSTANT:
        cout << "CONSTANT        " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_BUILT_IN:
        cout << "BUILT_IN        " << GET_A(ins) << " -> " << GET_B(ins);
        break;
        
      case OP_METHOD:
        cout << "METHOD          " << GET_A(ins) << " <- " << GET_B(ins);
        break;
        
      case OP_RECORD:
        cout << "RECORD          " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_LIST:
        cout << "LIST            [" << GET_A(ins) << "..." << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_GET_FIELD:
        cout << "GET_FIELD       " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;
        
      case OP_TEST_FIELD:
        cout << "TEST_FIELD      " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;

      case OP_GET_CLASS_FIELD:
        cout << "GET_CLASS_FIELD " << GET_A(ins) << "[" << GET_B(ins) << "] -> " << GET_C(ins);
        break;

      case OP_SET_CLASS_FIELD:
        cout << "SET_CLASS_FIELD " << GET_A(ins) << "[" << GET_B(ins) << "] = " << GET_C(ins);
        break;

      case OP_GET_VAR:
        cout << "OP_GET_VAR      module " << GET_A(ins) << ", var " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_SET_VAR:
        cout << "OP_SET_VAR      module " << GET_A(ins) << ", var " << GET_B(ins) << " <- " << GET_C(ins);
        break;
        
      case OP_EQUAL:
        cout << "EQUAL           " << GET_A(ins) << " == " << GET_B(ins) << " -> " << GET_C(ins);
        break;
        
      case OP_NOT:
        cout << "NOT             " << GET_A(ins);
        break;
        
      case OP_IS:
        cout << "IS              " << GET_A(ins) << " is " << GET_B(ins);
        break;
        
      case OP_JUMP:
        cout << "JUMP            " << GET_A(ins) << " " << GET_B(ins);
        break;
        
      case OP_JUMP_IF_FALSE:
        cout << "JUMP_IF_FALSE   " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_JUMP_IF_TRUE:
        cout << "JUMP_IF_TRUE    " << GET_A(ins) << "? " << GET_B(ins);
        break;
        
      case OP_CALL:
        cout << "CALL            " << GET_A(ins) << "(" << GET_B(ins) << ") -> " << GET_C(ins);
        break;
        
      case OP_NATIVE:
        cout << "NATIVE          " << GET_A(ins) << "(" << GET_B(ins) << ") -> " << GET_C(ins);
        break;
        
      case OP_RETURN:
        cout << "RETURN          " << GET_A(ins);
        break;
        
      case OP_THROW:
        cout << "THROW           " << GET_A(ins);
        break;
        
      case OP_ENTER_TRY:
        cout << "ENTER_TRY       " << GET_A(ins);
        break;
        
      case OP_EXIT_TRY:
        cout << "EXIT_TRY        ";
        break;
        
      case OP_TEST_MATCH:
        cout << "TEST_MATCH      " << GET_A(ins);
        break;
    }
    
    cout << endl;
  }

  void Chunk::reach()
  {
    constants_.reach();
  }

  void Method::reach()
  {
    def_.reach();
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
      // Determine their specialization order.
      sort(vm);
      ErrorReporter reporter;
      
      chunk_ = Compiler::compileMultimethod(vm, reporter, *this);
    }
    
    return chunk_;
  }

  void Multimethod::reach()
  {
    signature_.reach();
    chunk_.reach();
    methods_.reach();
  }

  void Multimethod::sort(VM& vm)
  {
    Array<gc<Method> > sorted;

    // TODO(bob): This is a really primitive topological sort. We end up
    // comparing the same set of methods multiple times. Could do lots better.
    while (sorted.count() < methods_.count())
    {
      for (int i = 0; i < methods_.count(); i++)
      {
        if (methods_[i].isNull()) continue;

        // See if any other method is more specialized than this one.
        bool covered = false;
        for (int j = 0; j < methods_.count(); j++)
        {
          if (i == j) continue;
          if (methods_[j].isNull()) continue;
          
          MethodOrder order = compare(methods_[i], methods_[j]);
          if (order == ORDER_AFTER)
          {
            covered = true;
            break;
          }
        }

        if (!covered)
        {
          sorted.add(methods_[i]);
          methods_[i] = NULL;
        }
      }
    }

    methods_.clear();
    methods_.addAll(sorted);
  }

  MethodOrder Multimethod::compare(gc<Method> a, gc<Method> b)
  {
    Array<MethodOrder> orders;
    if (!a->def()->leftParam().isNull())
    {
      orders.add(PatternComparer::compare(
          a->def()->leftParam(), b->def()->leftParam()));
    }

    if (!a->def()->rightParam().isNull())
    {
      orders.add(PatternComparer::compare(
          a->def()->rightParam(), b->def()->rightParam()));
    }

    if (!a->def()->value().isNull())
    {
      orders.add(PatternComparer::compare(
          a->def()->value(), b->def()->value()));
    }

    // Orderings have to agree or there isn't a well-defined order.
    MethodOrder order = unifyOrders(orders);

    /*
    std::cout << a->def() << std::endl;
    std::cout << b->def() << std::endl;
    std::cout << "order ";

    switch (order)
    {
      case ORDER_BEFORE: std::cout << "before"; break;
      case ORDER_AFTER: std::cout << "after"; break;
      case ORDER_NONE: std::cout << "none"; break;
      case ORDER_EQUAL: std::cout << "equal"; break;
    }
    
    std::cout << std::endl;
    */
    
    return order;
  }

  MethodOrder Multimethod::unifyOrders(const Array<MethodOrder>& orders)
  {
    MethodOrder order = ORDER_NONE;

    for (int i = 0; i < orders.count(); i++)
    {
      switch (orders[i])
      {
        case ORDER_BEFORE:
          // If orders disagree, then there is no ordering.
          if (order == ORDER_AFTER) return ORDER_NONE;
          order = ORDER_BEFORE;
          break;

        case ORDER_AFTER:
          // If orders disagree, then there is no ordering.
          if (order == ORDER_BEFORE) return ORDER_NONE;
          order = ORDER_AFTER;
          break;

        case ORDER_NONE:
          // Do nothing.
          break;

        case ORDER_EQUAL:
          if (order == ORDER_NONE) order = ORDER_EQUAL;
          break;
      }
    }

    return order;
  }

  MethodOrder PatternComparer::compare(gc<Pattern> a, gc<Pattern> b)
  {
    VariablePattern* variable = b->asVariablePattern();
    if (variable != NULL)
    {
      b = variable->pattern();
      // TODO(bob): Hackish. Treat variables without inner patterns as wildcards
      // by manually creating a WildcardPattern. Lame.
      if (b.isNull())
      {
        b = new WildcardPattern(variable->pos());
      }
    }

    MethodOrder result = ORDER_NONE;
    PatternComparer comparer(*b, &result);
    a->accept(comparer, -1);
    
    return result;
  }

  void PatternComparer::visit(RecordPattern& node, int dummy)
  {
    if (other_.asRecordPattern() != NULL) {
      ASSERT(false, "Not implemented.");
    } else if (other_.asTypePattern() != NULL) {
      // TODO(bob): Is this right?
      *result_ = ORDER_NONE;
    } else if (other_.asValuePattern() != NULL) {
      *result_ = ORDER_AFTER;
    } else if (other_.asWildcardPattern() != NULL) {
      *result_ = ORDER_BEFORE;
    } else {
      ASSERT(false, "Unknown pattern type.");
    }
  }

  void PatternComparer::visit(TypePattern& node, int dummy)
  {
    if (other_.asRecordPattern() != NULL) {
      // TODO(bob): Is this right?
      *result_ = ORDER_NONE;
    } else if (other_.asTypePattern() != NULL) {
      // TODO(bob): Should compare type relations in hierarchy.
      *result_ = ORDER_EQUAL;
    } else if (other_.asValuePattern() != NULL) {
      *result_ = ORDER_AFTER;
    } else if (other_.asWildcardPattern() != NULL) {
      *result_ = ORDER_BEFORE;
    } else {
      ASSERT(false, "Unknown pattern type.");
    }
  }

  void PatternComparer::visit(ValuePattern& node, int dummy)
  {
    if (other_.asRecordPattern() != NULL) {
      *result_ = ORDER_NONE;
    } else if (other_.asTypePattern() != NULL) {
      *result_ = ORDER_BEFORE;
    } else if (other_.asValuePattern() != NULL) {
      // TODO(bob): Check for value collisions.
      *result_ = ORDER_EQUAL;
    } else if (other_.asWildcardPattern() != NULL) {
      *result_ = ORDER_BEFORE;
    } else {
      ASSERT(false, "Unknown pattern type.");
    }
  }

  void PatternComparer::visit(VariablePattern& node, int dummy)
  {
    // TODO(bob): Hackish. Treat variables without inner patterns as wildcards
    // by manually creating a WildcardPattern. Lame.
    if (node.pattern().isNull())
    {
      (new WildcardPattern(node.pos()))->accept(*this, dummy);
      return;
    }

    // Just ignore the variable and compare the inner pattern.
    node.pattern()->accept(*this, dummy);
  }
  
  void PatternComparer::visit(WildcardPattern& node, int dummy)
  {
    if (other_.asRecordPattern() != NULL) {
      *result_ = ORDER_AFTER;
    } else if (other_.asTypePattern() != NULL) {
      *result_ = ORDER_AFTER;
    } else if (other_.asValuePattern() != NULL) {
      *result_ = ORDER_AFTER;
    } else if (other_.asWildcardPattern() != NULL) {
      *result_ = ORDER_EQUAL;
    } else {
      ASSERT(false, "Unknown pattern type.");
    }
  }
}