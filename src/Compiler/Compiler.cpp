#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "Node.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  void Compiler::compileModule(VM& vm, gc<ModuleAst> module,
                               ErrorReporter& reporter)
  {
    // Declare methods first so we can resolve mutually recursive calls.
    for (int i = 0; i < module->methods().count(); i++)
    {
      const MethodAst& methodAst = *module->methods()[i];
      vm.methods().declare(methodAst.name());
    }
    
    // Try to compile all of the methods.
    for (int i = 0; i < module->methods().count(); i++)
    {
      gc<MethodAst> methodAst = module->methods()[i];
      gc<Method> method = compileMethod(vm, methodAst, reporter);

      // Bail if there was a compile error.
      if (method.isNull()) return;
      
      vm.methods().define(methodAst->name(), method);
    }
  }
  
  gc<Method> Compiler::compileMethod(VM& vm, gc<MethodAst> methodAst,
                                       ErrorReporter& reporter)
  {
    Compiler compiler(vm, reporter);
    return compiler.compile(methodAst);
  }
  
  Compiler::Compiler(VM& vm, ErrorReporter& reporter)
  : NodeVisitor(),
    vm_(vm),
    reporter_(reporter),
    locals_(),
    code_(),
    constants_(),
    numTemps_(0),
    maxRegisters_(0)
  {}

  gc<Method> Compiler::compile(gc<MethodAst> method)
  {
    // Create a fake local for the argument and result value.
    int result = makeLocal(String::create("(return)"));
    
    // TODO(bob): Hackish and temporary.
    if (!method->parameter().isNull())
    {
      // Evaluate the method's parameter pattern.
      declarePattern(*method->parameter());
      method->parameter()->accept(*this, result);
    }
    
    method->body()->accept(*this, result);
    write(OP_END, result);
    
    return new Method(method->name(), code_, constants_, maxRegisters_);
  }
  
  void Compiler::visit(const BinaryOpNode& node, int dest)
  {
    switch (node.type())
    {
      case TOKEN_PLUS:      compileInfix(node, OP_ADD, dest); break;
      case TOKEN_MINUS:     compileInfix(node, OP_SUBTRACT, dest); break;
      case TOKEN_STAR:      compileInfix(node, OP_MULTIPLY, dest); break;
      case TOKEN_SLASH:     compileInfix(node, OP_DIVIDE, dest); break;
      case TOKEN_LESS_THAN: compileInfix(node, OP_LESS_THAN, dest); break;
        
      default:
        ASSERT(false, "Unknown infix operator.");
    }
  }
  
  void Compiler::visit(const BoolNode& node, int dest)
  {
    write(OP_BOOL, node.value() ? 1 : 0, dest);
  }
  
  void Compiler::visit(const CallNode& node, int dest)
  {
    int method = vm_.methods().find(node.name());
    if (method == -1)
    {
      reporter_.error(node.pos(), "Method '%s' is not defined.",
                      node.name()->cString());
    }
    
    ASSERT(node.leftArg().isNull(), "Left-hand arguments aren't supported yet.");
    
    // Compile the argument. Do this even if the method wasn't found so we can
    // report errors in the arg expression too.
    node.rightArg()->accept(*this, dest);
    
    if (method == -1) return;
    
    write(OP_CALL, method, dest);
  }
  
  void Compiler::visit(const DefMethodNode& node, int dest)
  {
    ASSERT(false, "Not implemented.");
  }
  
  void Compiler::visit(const DoNode& node, int dest)
  {
    // Keep track of locals that are declared in this scope.
    int numLocals = startScope();
    
    node.body()->accept(*this, dest);
    
    endScope(numLocals);
  }
  
  void Compiler::visit(const IfNode& node, int dest)
  {
    // TODO(bob): Should create scopes for the arms.
    
    // Compile the condition.
    node.condition()->accept(*this, dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToElse = startJump();
    
    // Compile the then arm.
    int numLocals = startScope();
    node.thenArm()->accept(*this, dest);
    endScope(numLocals);
    
    // Leave a space for the then arm to jump over the else arm.
    int jumpPastElse = startJump();
    
    // Compile the else arm.
    endJump(jumpToElse, OP_JUMP_IF_FALSE, dest, code_.count() - jumpToElse - 1);
      
    if (!node.elseArm().isNull())
    {
      int numLocals = startScope();
      node.elseArm()->accept(*this, dest);
      endScope(numLocals);
    }
    else
    {
      // TODO(bob): Should compile to `nothing` when that is implemented.
      // For now, just emit a false.
      write(OP_BOOL, 0, dest);
    }
    
    endJump(jumpPastElse, OP_JUMP, code_.count() - jumpPastElse - 1);
  }
  
  void Compiler::visit(const NameNode& node, int dest)
  {
    int local = locals_.lastIndexOf(node.name());
    
    if (local == -1)
    {
      reporter_.error(node.pos(),
                      "Variable '%s' is not defined.", node.name()->cString());
      return;
    }
    
    write(OP_MOVE, local, dest);
  }
  
  void Compiler::visit(const NumberNode& node, int dest)
  {
    int index = compileConstant(node);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(const SequenceNode& node, int dest)
  {
    for (int i = 0; i < node.expressions().count(); i++)
    {
      // TODO(bob): Could compile all but the last expression with a special
      // sigil dest that means "won't use" and some nodes could check that to
      // omit some unnecessary instructions.
      node.expressions()[i]->accept(*this, dest);
    }
  }
  
  void Compiler::visit(const StringNode& node, int dest)
  {
    int index = compileConstant(node);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(const VariableNode& node, int dest)
  {
    // Reserve the locals up front. This way we'll compile the value to a slot
    // *after* them. This ensures locals always come before temporaries.
    declarePattern(*node.pattern());
    
    // Compile the value.
    int valueReg = makeTemp();
    node.value()->accept(*this, valueReg);
    
    // TODO(bob): Handle mutable variables.
    
    // Now pattern match on it.
    node.pattern()->accept(*this, valueReg);
    
    // Copy the final result.
    // TODO(bob): Omit this in cases where it won't be used. Most variable
    // declarations are just in sequences.
    write(OP_MOVE, valueReg, dest);
    
    releaseTemp(); // valueReg.
  }
  
  void Compiler::visit(const VariablePattern& pattern, int value)
  {
    int variable = locals_.lastIndexOf(pattern.name());
    ASSERT(variable != -1, "Should have called declareVariables() already.")
    
    // Copy the value into the new variable.
    write(OP_MOVE, value, variable);
  }

  void Compiler::compileInfix(const BinaryOpNode& node, OpCode op, int dest)
  {
    int a = compileExpressionOrConstant(*node.left());
    int b = compileExpressionOrConstant(*node.right());
    
    write(op, a, b, dest);
    
    if (IS_REGISTER(a)) releaseTemp();
    if (IS_REGISTER(b)) releaseTemp();
  }
  
  int Compiler::compileExpressionOrConstant(const Node& node)
  {
    const NumberNode* number = node.asNumberNode();
    if (number != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*number));
    }
    
    const StringNode* string = node.asStringNode();
    if (string != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*string));
    }
    
    int dest = makeTemp();
      
    node.accept(*this, dest);
    return dest;
  }
  
  int Compiler::compileConstant(const NumberNode& node)
  {
    gc<Object> constant = new NumberObject(node.value());
    
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    constants_.add(constant);
    return constants_.count() - 1;
  }
  
  int Compiler::compileConstant(const StringNode& node)
  {
    gc<Object> constant = new StringObject(node.value());
    
    // TODO(bob): Should check for duplicates. Only need one copy of any
    // given constant.
    constants_.add(constant);
    return constants_.count() - 1;
  }
  
  void Compiler::declarePattern(const Pattern& pattern)
  {
    const VariablePattern* variablePattern = pattern.asVariablePattern();
    if (variablePattern != NULL)
    {
      makeLocal(variablePattern->name());
    }
  }
  
  void Compiler::write(OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);
    
    code_.add(MAKE_ABC(a, b, c, op));
  }
  
  int Compiler::startJump()
  {
    // Just write a dummy op to leave a space for the jump instruction.
    write(OP_MOVE);
    return code_.count() - 1;
  }
  
  void Compiler::endJump(int from, OpCode op, int a, int b, int c)
  {
    code_[from] = MAKE_ABC(a, b, c, op);
  }

  int Compiler::startScope()
  {
    return locals_.count();
  }
  
  void Compiler::endScope(int numLocals)
  {
    ASSERT(numTemps_ == 0, "Cannot end a scope when there are temporaries in "
           "use.");
    
    locals_.truncate(numLocals);
  }
  
  int Compiler::makeLocal(gc<String> name)
  {
    ASSERT(numTemps_ == 0, "Cannot declare a local variable when there are "
           "temporaries in use.");
    
    locals_.add(name);
    updateMaxRegisters();
    return locals_.count() - 1;
  }
  
  int Compiler::makeTemp()
  {
    numTemps_++;
    updateMaxRegisters();    
    return locals_.count() + numTemps_ - 1;
  }
  
  void Compiler::releaseTemp()
  {
    ASSERT(numTemps_ > 0, "No temp to release.");
    numTemps_--;
  }

  void Compiler::updateMaxRegisters()
  {
    if (maxRegisters_ < locals_.count() + numTemps_)
    {
      maxRegisters_ = locals_.count() + numTemps_;
    }
  }
}