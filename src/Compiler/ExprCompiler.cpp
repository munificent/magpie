#include "ErrorReporter.h"
#include "Method.h"
#include "ExprCompiler.h"
#include "Object.h"
#include "Resolver.h"

namespace magpie
{
  ExprCompiler::ExprCompiler(Compiler& compiler)
  : ExprVisitor(),
    compiler_(compiler),
    module_(NULL),
    chunk_(new Chunk()),
    code_(),
    numLocals_(0),
    numTemps_(0),
    maxSlots_(0)
  {}

  gc<Chunk> ExprCompiler::compileBody(Module* module, gc<Expr> body)
  {
    int maxLocals;
    int numClosures;
    Resolver::resolveBody(compiler_, *module, body, maxLocals, numClosures);
    compile(module, maxLocals, NULL, NULL, NULL, body);

    chunk_->setCode(code_, maxSlots_, numClosures);
    return chunk_;
  }

  gc<Chunk> ExprCompiler::compile(Multimethod& multimethod)
  {
    // Need at least one slot. If there are no methods (which can happen since
    // methods are forward-declared and can be called before any definition has
    // executed) we need one slot to load and throw the NoMethodError.
    maxSlots_ = 1;

    // TODO(bob): Lots of work needed here:
    // - Support call-next-method.
    // - Detect pattern collisions.
    // - Throw AmbiguousMethodError when appropriate.
    for (int i = 0; i < multimethod.methods().count(); i++)
    {
      gc<DefExpr> method = multimethod.methods()[i]->def();
      compile(multimethod.methods()[i]->module(),
              method->resolved().maxLocals(),
              method->leftParam(), method->rightParam(), method->value(),
              method->body());
    }

    // If we get here, all methods failed to match, so throw a NoMethodError.
    write(OP_BUILT_IN, 3, 0);
    write(OP_THROW, 0);

    // TODO(bob): If we ever allow nested methods, will need to handle upvars
    // here.
    chunk_->setCode(code_, maxSlots_, 0);
    return chunk_;
  }

  gc<Chunk> ExprCompiler::compile(Module* module, FnExpr& function)
  {
    compile(module, function.resolved().maxLocals(), NULL, function.pattern(),
            NULL, function.body());

    chunk_->setCode(code_, maxSlots_, function.resolved().closures().count());
    return chunk_;
  }

  gc<Chunk> ExprCompiler::compile(Module* module, AsyncExpr& expr)
  {
    compile(module, expr.resolved().maxLocals(), NULL, NULL, NULL, expr.body());

    chunk_->setCode(code_, maxSlots_, expr.resolved().closures().count());
    return chunk_;
  }

  void ExprCompiler::compile(Module* module, int maxLocals,
                             gc<Pattern> leftParam, gc<Pattern> rightParam,
                             gc<Pattern> valueParam, gc<Expr> body)
  {
    module_ = module;

    // Reserve slots up front for all of the locals. This ensures that
    // temps will always be after locals.
    // TODO(bob): Using max here isn't optimal. Ideally a given temp only
    // needs to be after the locals that are in scope during the duration
    // of that temp. But calculating that is a bit hairy. For now, until we
    // have a more advanced compiler, this is a simple solution.
    numLocals_ = maxLocals;
    maxSlots_ = MAX(maxSlots_, numLocals_);

    PatternCompiler compiler(*this, true);

    // Track the slots used for the arguments and result. This code here
    // must be kept carefully in sync with the similar prelude code in
    // Resolver.
    int numParamSlots = 0;

    // Evaluate the method's parameter patterns.
    compileParam(compiler, leftParam, numParamSlots);
    compileParam(compiler, rightParam, numParamSlots);
    compileParam(compiler, valueParam, numParamSlots);

    // The result slot is just after the param slots.
    compile(body, numParamSlots);

    write(OP_RETURN, numParamSlots);

    ASSERT(numTemps_ == 0, "Should not have any temps left.");

    compiler.endJumps();
  }
  
  void ExprCompiler::compileParam(PatternCompiler& compiler,
                                    gc<Pattern> param, int& slot)
  {
    // No parameter so do nothing.
    if (param.isNull()) return;

    RecordPattern* record = param->asRecordPattern();
    if (record != NULL)
    {
      // Compile each field.
      for (int i = 0; i < record->fields().count(); i++)
      {
        compileParamField(compiler, record->fields()[i].value, slot++);
      }
    }
    else
    {
      // If we got here, the pattern isn't a record, so it's a single slot.
      compileParamField(compiler, param, slot++);
    }
  }

  void ExprCompiler::compileParamField(PatternCompiler& compiler,
                                         gc<Pattern> param, int slot)
  {
    VariablePattern* variable = param->asVariablePattern();
    if (variable != NULL)
    {
      // It's a variable, so compile its inner pattern. We don't worry about
      // the variable itself because the calling convention ensures its value
      // is already in the right slot.
      compiler.compile(variable->pattern(), slot);
    }
    else
    {
      // Not a variable, so just compile it normally.
      compiler.compile(param, slot);
    }
  }

  int ExprCompiler::compileArg(gc<Expr> arg)
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

  void ExprCompiler::compile(gc<Expr> expr, int dest)
  {
    expr->accept(*this, dest);
  }

  void ExprCompiler::compile(gc<Pattern> pattern, int slot)
  {
    PatternCompiler compiler(*this);
    compiler.compile(pattern, slot);
  }

  void ExprCompiler::visit(AndExpr& expr, int dest)
  {
    compile(expr.left(), dest);

    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();

    compile(expr.right(), dest);

    endJump(jumpToEnd, OP_JUMP_IF_FALSE, dest);
  }

  void ExprCompiler::visit(AssignExpr& expr, int dest)
  {
    // Compile the value and also make it the result of the expression.
    compile(expr.value(), dest);

    // Now assign it to the left-hand side.
    expr.lvalue()->accept(*this, dest);
  }

  void ExprCompiler::visit(AsyncExpr& expr, int dest)
  {
    // TODO(bob): Handle closures.
    ExprCompiler compiler(compiler_);
    gc<Chunk> chunk = compiler.compile(module_, expr);
    int index = chunk_->addChunk(chunk);

    write(OP_ASYNC, index);
    // TODO(bob): What about dest?

    compileClosures(expr.resolved());
  }

  void ExprCompiler::visit(BinaryOpExpr& expr, int dest)
  {
    int a = compileExpressionOrConstant(expr.left());
    int b = compileExpressionOrConstant(expr.right());

    OpCode op;
    bool negate = false;
    switch (expr.type())
    {
      case TOKEN_EQEQ:   op = OP_EQUAL; break;
      case TOKEN_NEQ:    op = OP_EQUAL; negate = true; break;

      default:
        ASSERT(false, "Unknown infix operator.");
    }

    write(op, a, b, dest);

    if (negate) write(OP_NOT, dest);

    if (IS_SLOT(a)) releaseTemp();
    if (IS_SLOT(b)) releaseTemp();
  }

  void ExprCompiler::visit(BoolExpr& expr, int dest)
  {
    write(OP_BUILT_IN, expr.value() ? BUILT_IN_TRUE : BUILT_IN_FALSE, dest);
  }

  void ExprCompiler::visit(CallExpr& expr, int dest)
  {
    compileCall(expr, dest, -1);
  }

  void ExprCompiler::visit(CatchExpr& expr, int dest)
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

    endJump(jumpPastCatch, OP_JUMP, 1);
  }

  void ExprCompiler::visit(DefExpr& expr, int dest)
  {
    gc<String> signature = SignatureBuilder::build(expr);
    int multimethod = compiler_.findMethod(signature);
    methodId method = compiler_.addMethod(new Method(module_, &expr));

    write(OP_METHOD, multimethod, method);
  }

  void ExprCompiler::visit(DefClassExpr& expr, int dest)
  {
    // Create and load the class.
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);

    // Also store it in its named variable.
    compileAssignment(expr.resolved(), dest);

    // Compile the synthesized stuff.
    for (int i = 0; i < expr.synthesizedMethods().count(); i++)
    {
      expr.synthesizedMethods()[i]->accept(*this, dest);
    }
  }

  void ExprCompiler::visit(DoExpr& expr, int dest)
  {
    compile(expr.body(), dest);
  }

  void ExprCompiler::visit(FnExpr& expr, int dest)
  {
    // TODO(bob): Handle closures.
    ExprCompiler compiler(compiler_);
    gc<Chunk> chunk = compiler.compile(module_, expr);
    int index = chunk_->addChunk(chunk);

    write(OP_FUNCTION, index, dest);

    compileClosures(expr.resolved());
  }
  
  void ExprCompiler::visit(ForExpr& expr, int dest)
  {
    // TODO(bob): Hackish. An actual intermediate representation would help
    // here.
    int iterateMethod = compiler_.findMethod(String::create("0:iterate"));
    ASSERT(iterateMethod != -1, "Should have 'iterate' method in core.");

    int nextMethod = compiler_.findMethod(String::create("0:next"));
    ASSERT(nextMethod != -1, "Should have 'next' method in core.");

    int currentMethod = compiler_.findMethod(String::create("0:current"));
    ASSERT(currentMethod != -1, "Should have 'current' method in core.");

    // Evaluate the iteratable expression.
    int iterator = makeTemp();
    compile(expr.iterator(), iterator);

    // Then call "iterate" on it to get an iterator.
    // TODO(bob): Hackish. An actual intermediate representation would help
    // here.
    write(OP_CALL, iterateMethod, iterator, iterator);

    int loopStart = startJumpBack();

    // Call "next" on the iterator.
    write(OP_CALL, nextMethod, iterator, dest);

    // If false, jump to exit.
    int loopExit = startJump();

    // Call "current" on the iterator.
    write(OP_CALL, currentMethod, iterator, dest);

    // Match on the loop pattern.
    compile(expr.pattern(), dest);

    // Compile the body.
    compile(expr.body(), dest);

    endJumpBack(loopStart);
    endJump(loopExit, OP_JUMP_IF_FALSE, dest);

    releaseTemp(); // iterator.

    // TODO(bob): Need to figure out what the result value should be.
  }

  void ExprCompiler::visit(GetFieldExpr& expr, int dest)
  {
    write(OP_GET_CLASS_FIELD, expr.index());
  }

  void ExprCompiler::visit(IfExpr& expr, int dest)
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

    endJump(jumpPastElse, OP_JUMP, 1);
  }

  void ExprCompiler::visit(IsExpr& expr, int dest)
  {
    compile(expr.value(), dest);

    int type = makeTemp();
    compile(expr.type(), type);

    write(OP_IS, dest, type, dest);

    releaseTemp(); // type
  }

  void ExprCompiler::visit(ListExpr& expr, int dest)
  {
    // TODO(bob): Putting these all in registers and then copying them to the
    // list after creation may be slow. Another option to consider is to write
    // a start list bytecode first, and then have each element get pushed
    // directly to the new list.
    int firstElement = getNextTemp();
    for (int i = 0; i < expr.elements().count(); i++)
    {
      int element = makeTemp();
      compile(expr.elements()[i], element);
    }

    write(OP_LIST, firstElement, expr.elements().count(), dest);

    releaseTemps(expr.elements().count());
  }

  void ExprCompiler::visit(MatchExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    compileMatch(expr.cases(), dest);
  }

  void ExprCompiler::visit(NameExpr& expr, int dest)
  {
    ASSERT(expr.resolved()->isResolved(),
           "Names should be resolved before compiling.");

    switch (expr.resolved()->scope())
    {
      case NAME_LOCAL:
        write(OP_MOVE, expr.resolved()->index(), dest);
        break;

      case NAME_CLOSURE:
        write(OP_GET_UPVAR, expr.resolved()->index(), dest);
        break;

      case NAME_MODULE:
        write(OP_GET_VAR, expr.resolved()->module(), expr.resolved()->index(),
              dest);
        break;
    }
  }

  void ExprCompiler::visit(NativeExpr& expr, int dest)
  {
    write(OP_NATIVE, expr.index(), 0, dest);
  }

  void ExprCompiler::visit(NotExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    write(OP_NOT, dest);
  }

  void ExprCompiler::visit(NothingExpr& expr, int dest)
  {
    write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
  }

  void ExprCompiler::visit(NumberExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }

  void ExprCompiler::visit(OrExpr& expr, int dest)
  {
    compile(expr.left(), dest);

    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();

    compile(expr.right(), dest);

    endJump(jumpToEnd, OP_JUMP_IF_TRUE, dest);
  }

  void ExprCompiler::visit(RecordExpr& expr, int dest)
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
      names.add(compiler_.addSymbol(expr.fields()[i].name));
    }

    // TODO(bob): Need to sort field names.

    // Create the record type.
    int type = compiler_.addRecordType(names);

    // Create the record.
    write(OP_RECORD, firstField, type, dest);

    releaseTemps(expr.fields().count());
  }

  void ExprCompiler::visit(ReturnExpr& expr, int dest)
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

  void ExprCompiler::visit(SequenceExpr& expr, int dest)
  {
    for (int i = 0; i < expr.expressions().count(); i++)
    {
      // TODO(bob): Could compile all but the last expression with a special
      // sigil dest that means "won't use" and some exprs could check that to
      // omit some unnecessary instructions.
      compile(expr.expressions()[i], dest);
    }
  }

  void ExprCompiler::visit(SetFieldExpr& expr, int dest)
  {
    write(OP_SET_CLASS_FIELD, expr.index());
  }

  void ExprCompiler::visit(StringExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }

  void ExprCompiler::visit(ThrowExpr& expr, int dest)
  {
    // Compile the error object.
    compile(expr.value(), dest);

    // Throw it.
    write(OP_THROW, dest);
  }

  void ExprCompiler::visit(VariableExpr& expr, int dest)
  {
    // Compile the value.
    compile(expr.value(), dest);

    // Now pattern match on it.
    compile(expr.pattern(), dest);
  }

  void ExprCompiler::visit(WhileExpr& expr, int dest)
  {
    int loopStart = startJumpBack();

    // Compile the condition.
    int condition = makeTemp();
    compile(expr.condition(), condition);
    int loopExit = startJump();
    releaseTemp(); // condition

    compile(expr.body(), dest);
    endJumpBack(loopStart);
    endJump(loopExit, OP_JUMP_IF_FALSE, condition);
  }

  void ExprCompiler::visit(CallLValue& lvalue, int value)
  {
    // TODO(bob): Is overwriting the value slot correct here?
    compileCall(*lvalue.call(), value, value);
  }

  void ExprCompiler::visit(NameLValue& lvalue, int value)
  {
    compileAssignment(lvalue.resolved(), value);
  }

  void ExprCompiler::visit(RecordLValue& lvalue, int value)
  {
    // TODO(bob): Lot of copy/paste between this and RecordPattern.
    // Recurse into the fields.
    for (int i = 0; i < lvalue.fields().count(); i++)
    {
      // TODO(bob): Could be faster and skip this if the field is a wildcard.

      // Test and destructure the field. This takes two instructions to encode
      // all of the operands.
      int field = makeTemp();
      int symbol = compiler_.addSymbol(lvalue.fields()[i].name);

      write(OP_GET_FIELD, value, symbol, field);

      // Recurse into the record, using that field.
      lvalue.fields()[i].value->accept(*this, field);

      releaseTemp(); // field.
    }
  }

  void ExprCompiler::visit(WildcardLValue& lvalue, int value)
  {
    // Nothing to do.
  }

  void ExprCompiler::compileMatch(const Array<MatchClause>& clauses, int dest)
  {
    Array<int> endJumps;

    // Compile each case.
    for (int i = 0; i < clauses.count(); i++)
    {
      const MatchClause& clause = clauses[i];
      bool lastPattern = i == clauses.count() - 1;

      // Compile the pattern (if there is one and it isn't the "else" case).
      PatternCompiler compiler(*this, !lastPattern);
      if (!clause.pattern().isNull())
      {
        clause.pattern()->accept(compiler, dest);
      }

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
      endJump(endJumps[i], OP_JUMP, 1);
    }
  }

  int ExprCompiler::compileExpressionOrConstant(gc<Expr> expr)
  {
    const DefClassExpr* defClass = expr->asDefClassExpr();
    if (defClass != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*defClass));
    }

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

  int ExprCompiler::compileConstant(const DefClassExpr& expr)
  {
    return chunk_->addConstant(
        new ClassObject(expr.name(), expr.fields().count()));
  }

  int ExprCompiler::compileConstant(const NumberExpr& expr)
  {
    return chunk_->addConstant(new NumberObject(expr.value()));
  }

  int ExprCompiler::compileConstant(const StringExpr& expr)
  {
    return chunk_->addConstant(new StringObject(expr.value()));
  }

  void ExprCompiler::compileCall(const CallExpr& call, int dest,
                                   int valueSlot)
  {
    ASSERT(call.resolved() != -1,
           "Method should be resolved before compiling.");

    // Compile the method arguments.
    int firstArg = getNextTemp();
    int numTemps = 0;
    numTemps += compileArg(call.leftArg());
    numTemps += compileArg(call.rightArg());

    // Then add the value as the last argument.
    if (valueSlot != -1)
    {
      int valueArg = makeTemp();
      write(OP_MOVE, valueSlot, valueArg);
    }

    write(OP_CALL, call.resolved(), firstArg, dest);

    if (valueSlot != -1) releaseTemp(); // valueArg.
    releaseTemps(numTemps);
  }

  void ExprCompiler::compileAssignment(gc<ResolvedName> resolved, int value)
  {
    ASSERT(resolved->isResolved(), "Must resolve before compiling.");

    switch (resolved->scope())
    {
      case NAME_LOCAL:
        // Copy the value into the new variable.
        write(OP_MOVE, value, resolved->index());
        break;

      case NAME_CLOSURE:
        write(OP_SET_UPVAR, resolved->index(), value);
        break;

      case NAME_MODULE:
        // Assign to the top-level variable.
        write(OP_SET_VAR, resolved->module(), resolved->index(), value);
        break;
    }
  }

  void ExprCompiler::compileClosures(ResolvedProcedure& procedure)
  {
    // Capture variables from outer scopes.
    for (int i = 0; i < procedure.closures().count(); i++)
    {
      int index = procedure.closures()[i];
      if (index == -1)
      {
        // This closure is a new one in this function so there's nothing to
        // Capture. We just need to create a fresh upvar.
        write(OP_MOVE);
      }
      else
      {
        // Capture the upvar from the outer scope.
        write(OP_GET_UPVAR, index);
      }
    }
  }
  
  void ExprCompiler::write(OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);

    code_.add(MAKE_ABC(a, b, c, op));
  }

  int ExprCompiler::startJump()
  {
    // Just write a dummy op to leave a space for the jump instruction.
    write(OP_MOVE);
    return code_.count() - 1;
  }

  int ExprCompiler::startJumpBack()
  {
    // Remember the current instruction so we can calculate the offset.
    return code_.count() - 1;
  }

  void ExprCompiler::endJump(int from, OpCode op, int a, int b)
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

  void ExprCompiler::endJumpBack(int to)
  {
    int offset = code_.count() - to;
    write(OP_JUMP, 0, offset);
  }

  int ExprCompiler::getNextTemp() const
  {
    return numLocals_ + numTemps_;
  }

  int ExprCompiler::makeTemp()
  {
    numTemps_++;
    if (maxSlots_ < numLocals_ + numTemps_)
    {
      maxSlots_ = numLocals_ + numTemps_;
    }

    return numLocals_ + numTemps_ - 1;
  }

  void ExprCompiler::releaseTemp()
  {
    releaseTemps(1);
  }

  void ExprCompiler::releaseTemps(int count)
  {
    ASSERT(numTemps_ >= count, "Cannot release more temps than were created.");
    numTemps_ -= count;
  }

  void PatternCompiler::compile(gc<Pattern> pattern, int slot)
  {
    if (pattern.isNull()) return;

    pattern->accept(*this, slot);
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
        compiler_.endJump(test.position, OP_JUMP, 1);
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
      int symbol = compiler_.compiler_.addSymbol(pattern.fields()[i].name);

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
    compiler_.compileAssignment(pattern.resolved(), value);

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
}
