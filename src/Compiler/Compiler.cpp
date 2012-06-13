#include "Ast.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "Module.h"
#include "Object.h"
#include "Resolver.h"
#include "VM.h"

namespace magpie
{
  Module* Compiler::compileModule(VM& vm, gc<ModuleAst> moduleAst,
                                  ErrorReporter& reporter)
  {
    Module* module = new Module();
    
    // TODO(bob): Doing this here is hackish. Need to figure out when a module's
    // imports are resolved.
    module->imports().add(vm.coreModule());
    
    // Declare the definitions.
    for (int i = 0; i < moduleAst->defs().count(); i++)
    {
      // TODO(bob): Handle non-method defs when they exist.
      MethodDef* method = moduleAst->defs()[i]->asMethodDef();
      gc<String> signature = SignatureBuilder::build(*method);
      vm.methods().declare(signature);
    }
    
    // TODO(bob): This will need some work when modules and imports are
    // supported. We will need to forward declare all of the modules, handle
    // imported/exported names, and *then* go back and define all of them.
    // Now define them to allow mutual recursion.
    for (int i = 0; i < moduleAst->defs().count(); i++)
    {
      // TODO(bob): Handle non-method defs when they exist.
      MethodDef* method = moduleAst->defs()[i]->asMethodDef();
      gc<String> signature = SignatureBuilder::build(*method);
      gc<Method> compiled = compileMethod(vm, module, *method, reporter);
      vm.methods().define(signature, compiled);
    }
    
    // Wrap the body in a shell method and compile it.
    gc<Expr> body = moduleAst->body();
    MethodDef* method = new MethodDef(body->pos(), gc<Pattern>(),
        String::create("<module>"), gc<Pattern>(), body);
    
    module->bindBody(compileMethod(vm, module, *method, reporter));
    
    return module;
  }

  gc<Method> Compiler::compileMethod(VM& vm, Module* module,
                                     MethodDef& method,
                                     ErrorReporter& reporter)
  {
    Compiler compiler(vm, reporter, module);
    return compiler.compile(method);
  }
  
  Compiler::Compiler(VM& vm, ErrorReporter& reporter, Module* module)
  : ExprVisitor(),
    vm_(vm),
    reporter_(reporter),
    method_(new Method(module)),
    code_(),
    numLocals_(0),
    numTemps_(0),
    maxSlots_(0)
  {}

  gc<Method> Compiler::compile(MethodDef& method)
  {
    Resolver::resolve(reporter_, *method_->module(), method);
    
    // Reserve slots up front for all of the locals. This ensures that temps
    // will always be after locals.
    // TODO(bob): Using max here isn't optimal. Ideally a given temp only needs
    // to be after the locals that are in scope during the duration of that
    // temp. But calculating that is a bit hairy. For now, until we have a more
    // advanced compiler, this is a simple solution.
    numLocals_ = method.maxLocals();
    maxSlots_ = numLocals_;
    
    // Track the slots used for the arguments and result. This code here must
    // be kept carefully in sync with the similar prelude code in Resolver.
    int numParamSlots = 0;

    // Evaluate the method's parameter patterns.
    compileParam(method.leftParam(), numParamSlots);
    compileParam(method.rightParam(), numParamSlots);

    // The result slot is just after the param slots.
    compile(method.body(), numParamSlots);
    write(OP_RETURN, numParamSlots);

    method_->setCode(code_, maxSlots_);
    
    return method_;
  }
  
  void Compiler::compileParam(gc<Pattern> param, int& slot)
  {
    // No parameter so do nothing.
    if (param.isNull()) return;
    
    RecordPattern* record = param->asRecordPattern();
    if (record != NULL)
    {
      // Compile each field.
      for (int i = 0; i < record->fields().count(); i++)
      {
        compileParamField(record->fields()[i].value, slot++);
      }
    }
    else
    {
      // If we got here, the pattern isn't a record, so it's a single slot.
      compileParamField(param, slot++);
    }
  }

  void Compiler::compileParamField(gc<Pattern> param, int slot)
  {
    VariablePattern* variable = param->asVariablePattern();
    if (variable != NULL)
    {
      // It's a variable, so compile its inner pattern. We don't worry about
      // the variable itself because the calling convention ensures its value
      // is already in the right slot.
      compile(variable->pattern(), slot);
    }
    else
    {
      // Not a variable, so just compile it normally.
      compile(param, slot);
    }
  }
  
  int Compiler::compileArg(gc<Expr> arg)
  {
    // No arg so do nothing.
    if (arg.isNull()) return 0;
    
    RecordExpr* record = arg->asRecordExpr();
    if (record != NULL)
    {
      // Compile each field.
      for (int i = 0; i < record->fields().count(); i++)
      {
        compile(record->fields()[i].value, makeTemp());
      }
      
      return record->fields().count();
    }

    // If we got here, the arg isn't a record, so its a single value.
    compile(arg, makeTemp());
    return 1;
  }
    
  void Compiler::compile(gc<Expr> expr, int dest)
  {
    expr->accept(*this, dest);
  }
  
  void Compiler::compile(gc<Pattern> pattern, int slot)
  {
    if (pattern.isNull()) return;
    
    PatternCompiler compiler(*this);
    pattern->accept(compiler, slot);
  }
  
  void Compiler::visit(AndExpr& expr, int dest)
  {
    compile(expr.left(), dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    compile(expr.right(), dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_FALSE, dest);
  }
  
  void Compiler::visit(BinaryOpExpr& expr, int dest)
  {
    int a = compileExpressionOrConstant(expr.left());
    int b = compileExpressionOrConstant(expr.right());

    OpCode op;
    bool negate = false;
    switch (expr.type())
    {
      case TOKEN_PLUS:   op = OP_ADD; break;
      case TOKEN_MINUS:  op = OP_SUBTRACT; break;
      case TOKEN_STAR:   op = OP_MULTIPLY; break;
      case TOKEN_SLASH:  op = OP_DIVIDE; break;
      case TOKEN_EQEQ:   op = OP_EQUAL; break;
      case TOKEN_NEQ:    op = OP_EQUAL; negate = true; break;
      case TOKEN_LT:     op = OP_LESS_THAN; break;
      case TOKEN_LTE:    op = OP_GREATER_THAN; negate = true; break;
      case TOKEN_GT:     op = OP_GREATER_THAN; break;
      case TOKEN_GTE:    op = OP_LESS_THAN; negate = true; break;

      default:
        ASSERT(false, "Unknown infix operator.");
    }
    
    write(op, a, b, dest);
    
    if (negate) write(OP_NOT, dest);
    
    if (IS_SLOT(a)) releaseTemp();
    if (IS_SLOT(b)) releaseTemp();
  }

  void Compiler::visit(BoolExpr& expr, int dest)
  {
    write(OP_BUILT_IN, expr.value() ? BUILT_IN_TRUE : BUILT_IN_FALSE, dest);
  }

  void Compiler::visit(CallExpr& expr, int dest)
  {
    gc<String> signature = SignatureBuilder::build(expr);

    int method = vm_.methods().find(signature);
    
    if (method == -1)
    {
      reporter_.error(expr.pos(),
                      "Could not find a method with signature '%s'.",
                      signature->cString());
    
      // Just pick a method so we can keep compiling to report later errors.
      method = 0;
    }

    int firstArg = getNextTemp();
    int numTemps = 0;
    numTemps += compileArg(expr.leftArg());
    numTemps += compileArg(expr.rightArg());
    
    write(OP_CALL, method, firstArg, dest);
    
    releaseTemps(numTemps);
  }
  
  void Compiler::visit(CatchExpr& expr, int dest)
  {
    // Register the catch handler.
    int enter = startJump();
    
    // Compile the block body.
    compile(expr.body(), dest);
    
    // Complete the catch handler.
    write(OP_EXIT_TRY); // slot for caught value here?
    
    // Jump past it if an exception is not thrown.
    int jumpPastCatch = startJump();
    
    endJump(enter, OP_ENTER_TRY);
    
    // TODO(bob): Can we use "dest" here or does it need to be a temp?
    // Write a pseudo-opcode so we know what slot to put the error in.
    write(OP_MOVE, dest);
    
    compileMatch(expr.catches(), dest);
    
    endJump(jumpPastCatch, OP_JUMP);
  }
    
  void Compiler::visit(DoExpr& expr, int dest)
  {
    compile(expr.body(), dest);
  }

  void Compiler::visit(IfExpr& expr, int dest)
  {
    // Compile the condition.
    compile(expr.condition(), dest);

    // Leave a space for the test and jump instruction.
    int jumpToElse = startJump();

    // Compile the then arm.
    compile(expr.thenArm(), dest);
    
    // Leave a space for the then arm to jump over the else arm.
    int jumpPastElse = startJump();

    // Compile the else arm.
    endJump(jumpToElse, OP_JUMP_IF_FALSE, dest);

    if (!expr.elseArm().isNull())
    {
      compile(expr.elseArm(), dest);
    }
    else
    {
      // A missing 'else' arm is implicitly 'nothing'.
      write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
    }

    endJump(jumpPastElse, OP_JUMP);
  }
  
  void Compiler::visit(IsExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    
    int type = makeTemp();
    compile(expr.type(), type);

    write(OP_IS, dest, type, dest);

    releaseTemp(); // type
  }
  
  void Compiler::visit(MatchExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    compileMatch(expr.cases(), dest);
  }
  
  void Compiler::visit(NameExpr& expr, int dest)
  {
    ASSERT(expr.resolved().isResolved(),
           "Names should be resolved before compiling.");
    
    if (expr.resolved().isLocal())
    {
      write(OP_MOVE, expr.resolved().index(), dest);
    }
    else
    {
      write(OP_GET_MODULE, expr.resolved().import(), expr.resolved().index(),
            dest);
    }
  }
  
  void Compiler::visit(NotExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    write(OP_NOT, dest);
  }
  
  void Compiler::visit(NothingExpr& expr, int dest)
  {
    write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
  }

  void Compiler::visit(NumberExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(OrExpr& expr, int dest)
  {
    compile(expr.left(), dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    compile(expr.right(), dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_TRUE, dest);
  }
  
  void Compiler::visit(RecordExpr& expr, int dest)
  {
    // TODO(bob): Hack. This assumes that the fields in the expression are in
    // the same order that the type expects. Eventually, the type needs to sort
    // them so that it understands (x: 1, y: 2) and (y: 2, x: 1) are the same
    // shape. When that happens, this will need to take that into account.
    
    Array<int> names;
    
    // Compile the fields.
    int firstField = -1;
    for (int i = 0; i < expr.fields().count(); i++)
    {
      int fieldSlot = makeTemp();
      if (i == 0) firstField = fieldSlot;
      
      compile(expr.fields()[i].value, fieldSlot);
      names.add(vm_.addSymbol(expr.fields()[i].name));
    }
    
    // TODO(bob): Need to sort field names.
    
    // Create the record type.
    int type = vm_.addRecordType(names);
    
    // Create the record.
    write(OP_RECORD, firstField, type, dest);

    releaseTemps(expr.fields().count());
  }
  
  void Compiler::visit(ReturnExpr& expr, int dest)
  {
    // Compile the return value.
    if (expr.value().isNull())
    {
      // No value, so implicitly "nothing".
      write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
    }
    else
    {
      compile(expr.value(), dest);
    }

    write(OP_RETURN, dest);
  }
  
  void Compiler::visit(SequenceExpr& expr, int dest)
  {
    for (int i = 0; i < expr.expressions().count(); i++)
    {
      // TODO(bob): Could compile all but the last expression with a special
      // sigil dest that means "won't use" and some exprs could check that to
      // omit some unnecessary instructions.
      compile(expr.expressions()[i], dest);
    }
  }

  void Compiler::visit(StringExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(ThrowExpr& expr, int dest)
  {
    // Compile the error object.
    compile(expr.value(), dest);
    
    // Throw it.
    write(OP_THROW, dest);
  }
  
  void Compiler::visit(VariableExpr& expr, int dest)
  {
    // Compile the value.
    compile(expr.value(), dest);

    // TODO(bob): Handle mutable variables.

    // Now pattern match on it.
    compile(expr.pattern(), dest);
  }
  
  void Compiler::compileMatch(const Array<MatchClause>& clauses, int dest)
  {
    Array<int> endJumps;
    
    // Compile each case.
    for (int i = 0; i < clauses.count(); i++)
    {
      const MatchClause& clause = clauses[i];
      bool lastPattern = i == clauses.count() - 1;
      
      // Compile the pattern.
      PatternCompiler compiler(*this, !lastPattern);
      clause.pattern()->accept(compiler, dest);
      
      // Compile the body if the match succeeds.
      compile(clause.body(), dest);
      
      // Then jump past the other cases.
      if (!lastPattern)
      {
        endJumps.add(startJump());
        
        // If this pattern fails, make it jump to the next case.
        compiler.endJumps();
      }
    }
    
    // Patch all the jumps now that we know where the end is.
    for (int i = 0; i < endJumps.count(); i++)
    {
      endJump(endJumps[i], OP_JUMP);
    }
  }
  
  int Compiler::compileExpressionOrConstant(gc<Expr> expr)
  {
    const NumberExpr* number = expr->asNumberExpr();
    if (number != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*number));
    }

    const StringExpr* string = expr->asStringExpr();
    if (string != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*string));
    }

    int dest = makeTemp();

    compile(expr, dest);
    return dest;
  }

  int Compiler::compileConstant(const NumberExpr& expr)
  {
    return method_->addConstant(new NumberObject(expr.value()));
  }

  int Compiler::compileConstant(const StringExpr& expr)
  {
    return method_->addConstant(new StringObject(expr.value()));
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

  void Compiler::endJump(int from, OpCode op, int a, int b)
  {
    int c;
    int offset = code_.count() - from - 1;
    
    // Add the offset as the last operand.
    if (a == -1)
    {
      a = offset;
      b = 0xff;
      c = 0xff;
    }
    else if (b == -1)
    {
      b = offset;
      c = 0xff;
    }
    else
    {
      c = offset;
    }
    
    code_[from] = MAKE_ABC(a, b, c, op);
  }

  int Compiler::getNextTemp() const
  {
    return numLocals_ + numTemps_;
  }
  
  int Compiler::makeTemp()
  {
    numTemps_++;
    if (maxSlots_ < numLocals_ + numTemps_)
    {
      maxSlots_ = numLocals_ + numTemps_;
    }
    
    return numLocals_ + numTemps_ - 1;
  }

  void Compiler::releaseTemp()
  {
    releaseTemps(1);
  }
  
  void Compiler::releaseTemps(int count)
  {
    ASSERT(numTemps_ >= count, "Cannot release more temps than were created.");
    numTemps_ -= count;
  }
  
  void PatternCompiler::endJumps()
  {
    // Since this isn't the last case, then every match failure should just
    // jump to the next case.
    for (int j = 0; j < tests_.count(); j++)
    {
      const MatchTest& test = tests_[j];
      
      if (test.slot == -1)
      {
        // This test is a field destructure, so just set the offset.
        compiler_.endJump(test.position, OP_JUMP); 
      }
      else
      {
        // A normal test.
        compiler_.endJump(test.position, OP_JUMP_IF_FALSE, test.slot); 
      }
    }
  }
  
  void PatternCompiler::visit(RecordPattern& pattern, int value)
  {
    // Recurse into the fields.
    for (int i = 0; i < pattern.fields().count(); i++)
    {
      // Test and destructure the field. This takes two instructions to encode
      // all of the operands.
      int field = compiler_.makeTemp();
      int symbol = compiler_.vm_.addSymbol(pattern.fields()[i].name);
      
      if (jumpOnFailure_)
      {
        compiler_.write(OP_TEST_FIELD, value, symbol, field);
        tests_.add(MatchTest(compiler_.code_.count(), -1));
        compiler_.startJump();
      }
      else
      {
        compiler_.write(OP_GET_FIELD, value, symbol, field);
      }
      
      // Recurse into the pattern, using that field.
      pattern.fields()[i].value->accept(*this, field);
      
      compiler_.releaseTemp();
    }
  }
  
  void PatternCompiler::visit(TypePattern& pattern, int value)
  {
    // Evaluate the expected type.
    int expected = compiler_.makeTemp();
    compiler_.compile(pattern.type(), expected);
    
    // Test if the value matches the expected type.
    compiler_.write(OP_IS, value, expected, expected);
    writeTest(expected);
    
    compiler_.releaseTemp();
  }
  
  void PatternCompiler::visit(ValuePattern& pattern, int value)
  {
    // Evaluate the expected value.
    int expected = compiler_.makeTemp();
    compiler_.compile(pattern.value(), expected);
    
    // Test if the value matches the expected one.
    compiler_.write(OP_EQUAL, value, expected, expected);
    writeTest(expected);
    
    compiler_.releaseTemp();
  }
  
  void PatternCompiler::visit(VariablePattern& pattern, int value)
  {
    ASSERT(pattern.resolved().isResolved(),
           "Must resolve before compiling.");
    
    // Copy the value into the new variable.
    compiler_.write(OP_MOVE, value, pattern.resolved().index());
    
    // Compile the inner pattern.
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, value);
    }
  }
  
  void PatternCompiler::visit(WildcardPattern& pattern, int value)
  {
    // Nothing to do.
  }
  
  void PatternCompiler::writeTest(int slot)
  {
    if (jumpOnFailure_)
    {
      tests_.add(MatchTest(compiler_.code_.count(), slot));
    }
    compiler_.write(OP_TEST_MATCH, slot);
  }

  gc<String> SignatureBuilder::build(const CallExpr& expr)
  {
    // 1 foo                 -> ()foo
    // 1 foo()               -> ()foo
    // 1 foo(2)              -> ()foo()
    // foo(1)                -> foo()
    // (1, 2) foo            -> (,)foo
    // foo(1, b: 2, 3, e: 4) -> foo(,b,,e)
    SignatureBuilder builder;
    
    if (!expr.leftArg().isNull())
    {
      builder.writeArg(expr.leftArg());
      builder.add(" ");
    }
    
    builder.add(expr.name()->cString());

    if (!expr.rightArg().isNull())
    {
      builder.add(" ");
      builder.writeArg(expr.rightArg());
    }
    
    return String::create(builder.signature_, builder.length_);
  }
  
  gc<String> SignatureBuilder::build(const MethodDef& method)
  {
    // def (a) foo               -> ()foo
    // def (a) foo()             -> ()foo
    // def (a) foo(b)            -> ()foo()
    // def foo(b)                -> foo()
    // def (a, b) foo            -> (,)foo
    // def foo(a, b: c, d, e: f) -> foo(,b,,e)
    SignatureBuilder builder;
    
    if (!method.leftParam().isNull())
    {
      builder.writeParam(method.leftParam());
      builder.add(" ");
    }
    
    builder.add(method.name()->cString());
    
    if (!method.rightParam().isNull())
    {
      builder.add(" ");
      builder.writeParam(method.rightParam());
    }
    
    return String::create(builder.signature_, builder.length_);
  }
  
  void SignatureBuilder::writeArg(gc<Expr> expr)
  {
    // TODO(bob): Clean up. Redundant with build().
    // If it's a record, destructure it into the signature.
    RecordExpr* record = expr->asRecordExpr();
    if (record != NULL)
    {
      for (int i = 0; i < record->fields().count(); i++)
      {
        add(record->fields()[i].name);
        add(":");
      }
      
      return;
    }
    
    // Right now, all other exprs mean "some arg goes here".
    add("0:");
  }

  void SignatureBuilder::writeParam(gc<Pattern> pattern)
  {
    // If it's a record, destructure it into the signature.
    RecordPattern* record = pattern->asRecordPattern();
    if (record != NULL)
    {
      for (int i = 0; i < record->fields().count(); i++)
      {
        add(record->fields()[i].name);
        add(":");
      }
      
      return;
    }
    
    // Any other pattern is implicitly a single-field record.
    add("0:");
  }
  
  void SignatureBuilder::add(gc<String> text)
  {
    add(text->cString());
  }
  
  void SignatureBuilder::add(const char* text)
  {
    int length = strlen(text);
    ASSERT(length_ + length < MAX_LENGTH, "Signature too long.");
    
    strcpy(signature_ + length_, text);
    length_ += strlen(text);
  }
}