#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "MagpieString.h"
#include "Module.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  void Chunk::bind(int numSlots, int numUpvars)
  {
    numSlots_ = numSlots;
    numUpvars_ = numUpvars;
  }

  void Chunk::write(int file, int line, instruction ins)
  {
    code_.add(ins);
    codePos_.add(CodePos(file, line));
  }

  void Chunk::rewrite(int pos, instruction ins)
  {
    code_[pos] = ins;
  }

  int Chunk::addFile(gc<SourceFile> file)
  {
    // See if it's already in the list.
    for (int i = 0; i < files_.count(); i++)
    {
      if (&files_[i] == &file) return i;
    }

    files_.add(file);
    return files_.count() - 1;
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

  int Chunk::addChunk(gc<Chunk> chunk)
  {
    chunks_.add(chunk);
    return chunks_.count() - 1;
  }

  gc<Chunk> Chunk::getChunk(int index) const
  {
    ASSERT_INDEX(index, chunks_.count());
    return chunks_[index];
  }

  gc<SourceFile> Chunk::locateInstruction(int ip, int& line)
  {
    // Skip positions that don't have a file associated with them. For example,
    // when a multimethod fails to match an argument, the throw for that has
    // no label associated with it.
    if (codePos_[ip].file == -1) return NULL;

    line = codePos_[ip].line;
    return files_[codePos_[ip].file];
  }

  void Chunk::debugTrace(VM& vm) const
  {
    using namespace std;
    
    // TODO(bob): Constants.

    int file = -1;

    for (int i = 0; i < code_.count(); i++)
    {
      if (codePos_[i].file != file)
      {
        file = codePos_[i].file;
        std::cout << files_[file]->path() << std::endl;
      }

      std::cout << codePos_[i].line << " ";
      
      debugTrace(vm, code_[i]);
    }
  }
  
  void Chunk::debugTrace(VM& vm, instruction ins) const
  {
    using namespace std;

    int a = GET_A(ins);
    int b = GET_B(ins);
    int c = GET_C(ins);

    switch (GET_OP(ins))
    {
      case OP_MOVE:
        cout << "MOVE            " << a << " -> " << b;
        break;
        
      case OP_CONSTANT:
        cout << "CONSTANT        " << a << " -> " << b
             << " \"" << constants_[a] << "\"";
        break;
        
      case OP_BUILT_IN:
        cout << "BUILT_IN        " << a << " -> " << b;
        break;
        
      case OP_METHOD:
        cout << "METHOD          " << a << " <- " << b
             << " \"" << vm.getMultimethod(a)->signature() << "\"";
        break;
        
      case OP_RECORD:
        cout << "RECORD          " << a << "[" << b << "] -> " << c;
        break;
        
      case OP_LIST:
        cout << "LIST            [" << a << "..." << b << "] -> " << c;
        break;

      case OP_FUNCTION:
        cout << "FUNCTION        " << a << " -> " << b;
        break;
        
      case OP_ASYNC:
        cout << "ASYNC           " << a;
        break;

      case OP_CLASS:
        cout << "CLASS           " << vm.getSymbol(a) << " " << b
             << " fields -> " << c;
        break;

      case OP_GET_FIELD:
        cout << "GET_FIELD       " << a << "[" << b << "] -> " << c;
        break;
        
      case OP_TEST_FIELD:
        cout << "TEST_FIELD      " << a << "[" << b << "] -> " << c;
        break;

      case OP_GET_CLASS_FIELD:
        cout << "GET_CLASS_FIELD " << a << "[" << b << "] -> " << c;
        break;

      case OP_SET_CLASS_FIELD:
        cout << "SET_CLASS_FIELD " << a << "[" << b << "] = " << c;
        break;

      case OP_GET_VAR:
        cout << "OP_GET_VAR      module " << a << ", var " << b << " -> " << c;
        break;
        
      case OP_SET_VAR:
        cout << "OP_SET_VAR      module " << a << ", var " << b << " <- " << c;
        break;

      case OP_GET_UPVAR:
        cout << "OP_GET_UPVAR    " << a << " -> " << b;
        break;

      case OP_SET_UPVAR:
        cout << "OP_SET_UPVAR    " << a << " <- " << b;
        break;
        
      case OP_EQUAL:
        cout << "EQUAL           " << a << " == " << b << " -> " << c;
        break;
        
      case OP_NOT:
        cout << "NOT             " << a;
        break;
        
      case OP_IS:
        cout << "IS              " << a << " is " << b;
        break;
        
      case OP_JUMP:
        cout << "JUMP            " << a << " " << b;
        break;
        
      case OP_JUMP_IF_FALSE:
        cout << "JUMP_IF_FALSE   " << a << "? " << b;
        break;
        
      case OP_JUMP_IF_TRUE:
        cout << "JUMP_IF_TRUE    " << a << "? " << b;
        break;
        
      case OP_CALL:
      {
        gc<Multimethod> method = vm.getMultimethod(a);
        cout << "CALL            " << a << "(" << b << ") -> "
             << c << " \"" << method->signature() << "\"";
        break;
      }
        
      case OP_NATIVE:
        cout << "NATIVE          " << a << "(" << b << ") -> " << c;
        break;
        
      case OP_RETURN:
        cout << "RETURN          " << a;
        break;
        
      case OP_THROW:
        cout << "THROW           " << a;
        break;
        
      case OP_ENTER_TRY:
        cout << "ENTER_TRY       " << a;
        break;
        
      case OP_EXIT_TRY:
        cout << "EXIT_TRY        ";
        break;
        
      case OP_TEST_MATCH:
        cout << "TEST_MATCH      " << a;
        break;
    }
    
    cout << endl;
  }

  void Chunk::reach()
  {
    constants_.reach();
    chunks_.reach();
    files_.reach();
  }

  void Method::reach()
  {
    def_.reach();
  }

  Multimethod::Multimethod(gc<String> signature)
  : signature_(signature),
    function_(),
    methods_()
  {}
  
  void Multimethod::addMethod(gc<Method> method)
  {
    methods_.add(method);
    
    // Clear out the code since it needs to be recompiled.
    function_ = NULL;
  }
  
  gc<FunctionObject> Multimethod::getFunction(VM& vm)
  {
    // Re-compile if methods have been defined since the last time this was
    // called.
    if (function_.isNull())
    {
      // Determine their specialization order.
      sort(vm);
      ErrorReporter reporter;
      
      gc<Chunk> chunk = Compiler::compileMultimethod(vm, reporter, *this);
      function_ = FunctionObject::create(chunk);
    }
    
    return function_;
  }

  void Multimethod::reach()
  {
    signature_.reach();
    function_.reach();
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
          
          MethodOrder order = compare(vm, methods_[i], methods_[j]);
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

  MethodOrder Multimethod::compare(VM& vm, gc<Method> a, gc<Method> b)
  {
    Array<MethodOrder> orders;
    if (!a->def()->leftParam().isNull())
    {
      orders.add(PatternComparer::compare(vm,
          a->def()->leftParam(), b->def()->leftParam()));
    }

    if (!a->def()->rightParam().isNull())
    {
      orders.add(PatternComparer::compare(vm,
          a->def()->rightParam(), b->def()->rightParam()));
    }

    if (!a->def()->value().isNull())
    {
      orders.add(PatternComparer::compare(vm,
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

  MethodOrder PatternComparer::compare(VM& vm, gc<Pattern> a, gc<Pattern> b)
  {
    MethodOrder result = ORDER_NONE;
    PatternComparer comparer(vm, skipVariables(b), &result);
    a->accept(comparer, -1);
    
    return result;
  }

  void PatternComparer::visit(RecordPattern& node, int dummy)
  {
    if (other_.isNull())
    {
      *result_ = ORDER_BEFORE;
    }
    else if (other_->asRecordPattern() != NULL)
    {
      *result_ = compareRecords(node, *other_->asRecordPattern());
    }
    else if (other_->asTypePattern() != NULL)
    {
      // TODO(bob): Is this right?
      *result_ = ORDER_NONE;
    }
    else if (other_->asValuePattern() != NULL)
    {
      *result_ = ORDER_AFTER;
    }
    else
    {
      ASSERT(false, "Unknown pattern type.");
    }
  }

  void PatternComparer::visit(TypePattern& node, int dummy)
  {
    if (other_.isNull())
    {
      *result_ = ORDER_BEFORE;
      return;
    }

    if (other_->asRecordPattern() != NULL)
    {
      // TODO(bob): Is this right?
      *result_ = ORDER_NONE;
      return;
    }

    TypePattern* type = other_->asTypePattern();
    if (type != NULL)
    {
      // Compare type relations.
      // TODO(bob): Need to handle types not resolving to class objects.
      gc<ClassObject> a = asClass(getValue(node.type()));
      gc<ClassObject> b = asClass(getValue(type->type()));

      if (a.sameAs(b))
      {
        // Same class.
        *result_ = ORDER_EQUAL;
      }
      else if (a->is(*b))
      {
        // A is a subclass of B.
        *result_ = ORDER_BEFORE;
      }
      else if (b->is(*a))
      {
        // B is a subclass of A.
        *result_ = ORDER_AFTER;
      }
      else
      {
        // Unrelated types.
        *result_ = ORDER_NONE;
      }
      return;
    }

    if (other_->asValuePattern() != NULL)
    {
      *result_ = ORDER_AFTER;
      return;
    }

    ASSERT(false, "Unknown pattern type.");
  }

  void PatternComparer::visit(ValuePattern& node, int dummy)
  {
    if (other_.isNull())
    {
      *result_ = ORDER_BEFORE;
      return;
    }

    if (other_->asRecordPattern() != NULL)
    {
      *result_ = ORDER_NONE;
      return;
    }

    if (other_->asTypePattern() != NULL)
    {
      *result_ = ORDER_BEFORE;
      return;
    }

    ValuePattern* value = other_->asValuePattern();
    if (value != NULL)
    {
      // Check for collision.
      gc<Object> a = getValue(node.value());
      gc<Object> b = getValue(value->value());

      if (a->equals(b))
      {
        *result_ = ORDER_EQUAL;
      }
      else
      {
        *result_ = ORDER_NONE;
      }
      return;
    }

    ASSERT(false, "Unknown pattern type.");
  }

  void PatternComparer::visit(VariablePattern& node, int dummy)
  {
    gc<Pattern> pattern = skipVariables(node.pattern());

    if (pattern.isNull())
    {
      // No inner pattern, so this one is a wildcard.
      if (other_.isNull())
      {
        *result_ = ORDER_EQUAL;
      }
      else if (other_->asRecordPattern() != NULL)
      {
        *result_ = ORDER_AFTER;
      }
      else if (other_->asTypePattern() != NULL)
      {
        *result_ = ORDER_AFTER;
      }
      else if (other_->asValuePattern() != NULL)
      {
        *result_ = ORDER_AFTER;
      }
      else
      {
        ASSERT(false, "Unknown pattern type.");
      }
    }
    else
    {
      // Compare the inner pattern.
      pattern->accept(*this, dummy);
    }
  }

  gc<Pattern> PatternComparer::skipVariables(gc<Pattern> pattern)
  {
    while (!pattern.isNull())
    {
      VariablePattern* variable = pattern->asVariablePattern();
      if (variable == NULL) break;
      pattern = variable->pattern();
    }

    return pattern;
  }

  MethodOrder PatternComparer::compareRecords(RecordPattern& a,
                                              RecordPattern& b)
  {
    // Take the intersection of their fields.
    Array<gc<String> > intersect;
    int onlyInA = 0;
    int onlyInB = 0;
    for (int i = 0; i < a.fields().count(); i++)
    {
      bool inBoth = false;
      for (int j = 0; j < b.fields().count(); j++)
      {
        if (*a.fields()[i].name == *b.fields()[j].name)
        {
          inBoth = true;
          break;
        }
      }

      if (inBoth)
      {
        intersect.add(a.fields()[i].name);
      }
      else
      {
        onlyInA++;
      }
    }

    for (int j = 0; j < b.fields().count(); j++)
    {
      bool inBoth = false;
      for (int i = 0; i < a.fields().count(); i++)
      {
        if (*a.fields()[i].name == *b.fields()[j].name)
        {
          inBoth = true;
          break;
        }
      }

      if (!inBoth) onlyInB++;
    }

    // If the records don't have the same number of fields, one must be a
    // strict superset of the other.
    if ((onlyInA > 0) && (onlyInB > 0))
    {
      return ORDER_NONE;
    }

    // Which record are we leaning towards preferring? By default, we lean
    // towards the one with more fields.
    MethodOrder order = ORDER_EQUAL;
    if (onlyInA > 0) order = ORDER_BEFORE;
    else if (onlyInB > 0) order = ORDER_AFTER;

    // Fields that are common to the two cannot disagree on sort order.
    for (int i = 0; i < intersect.count(); i++)
    {
      gc<String> name = intersect[i];

      gc<Pattern> aField;
      for (int j = 0; j < a.fields().count(); j++)
      {
        if (*a.fields()[j].name == *name)
        {
          aField = a.fields()[j].value;
          break;
        }
      }

      gc<Pattern> bField;
      for (int j = 0; j < b.fields().count(); j++)
      {
        if (*b.fields()[j].name == *name)
        {
          bField = b.fields()[j].value;
          break;
        }
      }

      MethodOrder fieldOrder = compare(vm_, aField, bField);
      if (fieldOrder == ORDER_NONE) return ORDER_NONE;

      if (order == ORDER_EQUAL)
      {
        // We don't have an ordering yet, so take it from this field.
        order = fieldOrder;
      }
      else if (fieldOrder == ORDER_EQUAL)
      {
        // Do nothing.
      }
      else if (fieldOrder != order)
      {
        // The fields don't agree.
        return ORDER_NONE;
      }
    }

    return order;
  }

  gc<Object> PatternComparer::getValue(gc<Expr> expr)
  {
    // Handle literal values.
    BoolExpr* boolExpr = expr->asBoolExpr();
    if (boolExpr != NULL)
    {
      return new BoolObject(boolExpr->value());
    }

    CharacterExpr* charExpr = expr->asCharacterExpr();
    if (charExpr != NULL)
    {
      return new CharacterObject(charExpr->value());
    }

    FloatExpr* floatExpr = expr->asFloatExpr();
    if (floatExpr != NULL)
    {
      return new FloatObject(floatExpr->value());
    }

    IntExpr* intExpr = expr->asIntExpr();
    if (intExpr != NULL)
    {
      return new IntObject(intExpr->value());
    }

    StringExpr* stringExpr = expr->asStringExpr();
    if (stringExpr != NULL)
    {
      return new StringObject(stringExpr->value());
    }

    // Handle top-level names.
    NameExpr* name = expr->asNameExpr();
    if (name != NULL)
    {
      ASSERT(name->resolved()->scope() == NAME_MODULE,
             "Method patterns should only contain module-level names.");
      
      int moduleIndex = name->resolved()->module();
      int variableIndex = name->resolved()->index();
      
      Module* module = vm_.getModule(moduleIndex);
      gc<Object> object = module->getVariable(variableIndex);

      // TODO(bob): Handle undefined names.
      /*
       if (object.isNull())
       {
       gc<Object> error = DynamicObject::create(
       vm_.undefinedVarErrorClass());

       if (!throwError(error)) return FIBER_UNCAUGHT_ERROR;
       }
       */

      return object;
    }

    ASSERT(false, "Unexpected expression in method pattern.");
    return NULL;
  }
}
