#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "Module.h"
#include "Node.h"
#include "Object.h"
#include "VM.h"

namespace magpie
{
  Module* Compiler::compileModule(VM& vm, gc<Node> module,
                                     ErrorReporter& reporter)
  {
    // TODO(bob): Temp hackish. Wrap the module body in a fake method.
    DefMethodNode* method = new DefMethodNode(module->pos(),
        String::create("<module>"),
        new VariablePattern(module->pos(), String::create("<unused>")), module);

    gc<Method> body = compileMethod(vm, *method, reporter);
    return new Module(body);
  }

  gc<Method> Compiler::compileMethod(VM& vm, const DefMethodNode& method,
                                     ErrorReporter& reporter)
  {
    Compiler compiler(vm, reporter);
    return compiler.compile(method);
  }
  
  Compiler::Scope::Scope(Compiler* compiler)
  : compiler_(*compiler),
    parent_(compiler_.scope_),
    start_(compiler_.locals_.count())
  {
    compiler_.scope_ = this;
  }
  
  Compiler::Scope::~Scope()
  {
    ASSERT(start_ == -1, "Forgot to end scope.");
  }
  
  int Compiler::Scope::makeLocal(const SourcePos& pos, gc<String> name)
  {
    ASSERT(compiler_.numTemps_ == 0,
           "Cannot make a local variable when there are temporaries in use.");
    
    Array<gc<String> >& locals = compiler_.locals_;

    // Make sure there isn't already a local variable with this name in this
    // scope.
    for (int i = start_; i < locals.count(); i++)
    {
      if (locals[i] == name)
      {
        compiler_.reporter_.error(pos,
            "There is already a variable '%s' defined in this scope.",
            name->cString());
      }
    }
    
    compiler_.locals_.add(name);
    compiler_.updateMaxRegisters();
    return compiler_.locals_.count() - 1;
  }
  
  void Compiler::Scope::end()
  {
    ASSERT(start_ != -1, "Already ended this scope.");
    ASSERT(compiler_.numTemps_ == 0,
           "Cannot end a scope when there are temporaries in use.");
    
    compiler_.locals_.truncate(start_);
    compiler_.scope_ = parent_;
    start_ = -1;
  }
  
  void Compiler::Scope::visit(const NothingPattern& pattern, int unused)
  {
    // Nothing to do.
  }
  
  void Compiler::Scope::visit(const VariablePattern& pattern, int unused)
  {
    makeLocal(pattern.pos(), pattern.name());
  }
  
  Compiler::Compiler(VM& vm, ErrorReporter& reporter)
  : NodeVisitor(),
    vm_(vm),
    reporter_(reporter),
    method_(new Method()),
    locals_(),
    code_(),
    numTemps_(0),
    maxRegisters_(0),
    scope_(NULL)
  {}

  gc<Method> Compiler::compile(const DefMethodNode& method)
  {
    // Create a top-level scope.
    Scope scope(this);
    scope_ = &scope;
    
    // Create a fake local for the argument and result value.
    int result = scope.makeLocal(method.pos(), String::create("(return)"));

    // Evaluate the method's parameter pattern.
    reserveVariables(*method.parameter());
    method.parameter()->accept(*this, result);

    method.body()->accept(*this, result);
    write(OP_RETURN, result);

    method_->setCode(code_, maxRegisters_);
    
    scope.end();
    return method_;
  }
  
  void Compiler::visit(const AndNode& node, int dest)
  {
    node.left()->accept(*this, dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    node.right()->accept(*this, dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_FALSE, dest, code_.count() - jumpToEnd - 1);
  }
  
  void Compiler::visit(const BinaryOpNode& node, int dest)
  {
    int a = compileExpressionOrConstant(*node.left());
    int b = compileExpressionOrConstant(*node.right());

    OpCode op;
    switch (node.type())
    {
      case TOKEN_PLUS:      op = OP_ADD; break;
      case TOKEN_MINUS:     op = OP_SUBTRACT; break;
      case TOKEN_STAR:      op = OP_MULTIPLY; break;
      case TOKEN_SLASH:     op = OP_DIVIDE; break;
      case TOKEN_LESS_THAN: op = OP_LESS_THAN; break;

      default:
        ASSERT(false, "Unknown infix operator.");
    }
    
    write(op, a, b, dest);
    
    if (IS_REGISTER(a)) releaseTemp();
    if (IS_REGISTER(b)) releaseTemp();
  }

  void Compiler::visit(const BoolNode& node, int dest)
  {
    write(OP_BUILT_IN, node.value() ? BUILT_IN_TRUE : BUILT_IN_FALSE, dest);
  }

  void Compiler::visit(const CallNode& node, int dest)
  {
    int method = vm_.methods().find(node.name());
    if (method == -1)
    {
      // If we didn't find it, create an implicit forward declaration.
      // TODO(bob): After the module is compiled, should go back and ensure that
      // all forward declarations have been filled in.
      method = vm_.methods().declare(node.name());
    }

    ASSERT(node.leftArg().isNull(), "Left-hand arguments aren't supported yet.");

    // Compile the argument. Do this even if the method wasn't found so we can
    // report errors in the arg expression too.
    node.rightArg()->accept(*this, dest);

    write(OP_CALL, method, dest);
  }

  void Compiler::visit(const DefMethodNode& node, int dest)
  {
    // TODO(bob): Handle nested non-top-level methods.
    gc<Method> compiled = compileMethod(vm_, node, reporter_);
    int methodIndex = method_->addMethod(compiled);
    int globalIndex = vm_.methods().declare(node.name());
    
    write(OP_DEF_METHOD, methodIndex, globalIndex);
    
    // TODO(bob): Emit code to capture upvals and upvars.
  }

  void Compiler::visit(const DoNode& node, int dest)
  {
    Scope doScope(this);
    node.body()->accept(*this, dest);
    doScope.end();
  }

  void Compiler::visit(const IfNode& node, int dest)
  {
    Scope ifScope(this);

    // Compile the condition.
    node.condition()->accept(*this, dest);

    // Leave a space for the test and jump instruction.
    int jumpToElse = startJump();

    // Compile the then arm.
    Scope thenScope(this);
    node.thenArm()->accept(*this, dest);
    thenScope.end();
    
    // Leave a space for the then arm to jump over the else arm.
    int jumpPastElse = startJump();

    // Compile the else arm.
    endJump(jumpToElse, OP_JUMP_IF_FALSE, dest, code_.count() - jumpToElse - 1);

    if (!node.elseArm().isNull())
    {
      Scope elseScope(this);
      node.elseArm()->accept(*this, dest);
      elseScope.end();
    }
    else
    {
      // A missing 'else' arm is implicitly 'nothing'.
      write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
    }

    endJump(jumpPastElse, OP_JUMP, code_.count() - jumpPastElse - 1);
    ifScope.end();
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

  void Compiler::visit(const NothingNode& node, int dest)
  {
    write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
  }

  void Compiler::visit(const NumberNode& node, int dest)
  {
    int index = compileConstant(node);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(const OrNode& node, int dest)
  {
    node.left()->accept(*this, dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    node.right()->accept(*this, dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_TRUE, dest, code_.count() - jumpToEnd - 1);
  }
  
  void Compiler::visit(const ReturnNode& node, int dest)
  {
    // Compile the return value.
    if (node.value().isNull())
    {
      // No value, so implicitly "nothing".
      write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
    }
    else
    {
      node.value()->accept(*this, dest);
    }

    write(OP_RETURN, dest);
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
    reserveVariables(*node.pattern());

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
  
  void Compiler::visit(const NothingPattern& pattern, int value)
  {
    // TODO(bob): Eventually this should generate code to test the pattern.
  }
  
  void Compiler::visit(const VariablePattern& pattern, int value)
  {
    int variable = locals_.lastIndexOf(pattern.name());
    ASSERT(variable != -1, "Should have called declareVariables() already.")

    // Copy the value into the new variable.
    write(OP_MOVE, value, variable);
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
    return method_->addConstant(new NumberObject(node.value()));
  }

  int Compiler::compileConstant(const StringNode& node)
  {
    return method_->addConstant(new StringObject(node.value()));
  }

  void Compiler::reserveVariables(const Pattern& pattern)
  {
    // Int is unused.
    pattern.accept(*scope_, -1);
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