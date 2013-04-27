#include "Compiler/ExprCompiler.h"
#include "Compiler/Resolver.h"
#include "VM/Method.h"
#include "VM/Module.h"
#include "VM/Object.h"
#include "Syntax/ErrorReporter.h"
#include "Syntax/Token.h"

namespace magpie
{
  ExprCompiler::ExprCompiler(Compiler& compiler)
  : ExprVisitor(),
    compiler_(compiler),
    module_(NULL),
    chunk_(new Chunk()),
    numLocals_(0),
    numTemps_(0),
    maxSlots_(0),
    currentLoop_(NULL),
    currentFile_(-1)
  {}

  gc<Chunk> ExprCompiler::compileBody(Module* module, gc<Expr> body)
  {
    int maxLocals;
    int numClosures;
    Resolver::resolveBody(compiler_, *module, body, maxLocals, numClosures);

    if (compiler_.reporter().numErrors() == 0)
    {
      compile(module, maxLocals, NULL, NULL, NULL, body);
    }

    chunk_->bind(maxSlots_, numClosures);
    return chunk_;
  }

  gc<Chunk> ExprCompiler::compile(Multimethod& multimethod)
  {
    // Need at least one slot. If there are no methods (which can happen since
    // methods are forward-declared and can be called before any definition has
    // executed) we need one slot to load and throw the NoMethodError.
    maxSlots_ = 1;

    int numClosures = 0;

    // TODO(bob): Lots of work needed here:
    // - Support call-next-method.
    // - Detect pattern collisions.
    // - Throw AmbiguousMethodError when appropriate.
    for (int i = 0; i < multimethod.methods().count(); i++)
    {
      gc<Method> method = multimethod.methods()[i];
      gc<DefExpr> def = method->def();
      compile(method->module(),
              def->resolved().maxLocals(),
              def->leftParam(), def->rightParam(), def->value(),
              def->body());

      // Keep track of the total number of closures we need.
      numClosures = MAX(numClosures, def->resolved().closures().count());
    }

    // If we get here, all methods failed to match, so throw a NoMethodError.
    write(-1, OP_ATOM, ATOM_NO_METHOD, 0);
    write(-1, OP_THROW, 0);

    chunk_->bind(maxSlots_, numClosures);

    return chunk_;
  }

  gc<Chunk> ExprCompiler::compile(Module* module, FnExpr& function)
  {
    compile(module, function.resolved().maxLocals(),
            NULL, function.pattern(), NULL, function.body());

    // TODO(bob): Is this the right error?
    // If we get here, the argument didn't match the function's signature so
    // throw a NoMethodError.
    write(-1, OP_ATOM, ATOM_NO_METHOD, 0);
    write(-1, OP_THROW, 0);

    chunk_->bind(maxSlots_, function.resolved().closures().count());
    return chunk_;
  }

  gc<Chunk> ExprCompiler::compile(Module* module, AsyncExpr& expr)
  {
    compile(module, expr.resolved().maxLocals(),
            NULL, NULL, NULL, expr.body());

    chunk_->bind(maxSlots_, expr.resolved().closures().count());
    return chunk_;
  }

  void ExprCompiler::compile(Module* module, int maxLocals,
                             gc<Pattern> leftParam, gc<Pattern> rightParam,
                             gc<Pattern> valueParam, gc<Expr> body)
  {
    currentFile_ = chunk_->addFile(module->source());

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

    write(body->pos(), OP_RETURN, numParamSlots);

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

      // If we closed over the parameter, then we don't want in a local slot,
      // we want it in the upvar, so create it and copy the value up.
      if (*variable->name() != "_" &&
          variable->resolved()->scope() == NAME_CLOSURE)
      {
        write(variable->pos(),
              OP_SET_UPVAR, variable->resolved()->index(), slot, 1);
      }
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
    int jumpToEnd = startJump(expr);

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

    write(expr.pos(), OP_ASYNC, index);
    // TODO(bob): What about dest?

    compileClosures(expr.pos(), expr.resolved());
  }

  void ExprCompiler::visit(AtomExpr& expr, int dest)
  {
    write(expr, OP_ATOM, expr.atom(), dest);
  }

  void ExprCompiler::visit(BreakExpr& expr, int dummy)
  {
    currentLoop_->addBreak(expr);
  }

  void ExprCompiler::visit(CallExpr& expr, int dest)
  {
    compileCall(expr, dest, -1);
  }

  void ExprCompiler::visit(CatchExpr& expr, int dest)
  {
    // Register the catch handler so we know where to jump if the body throws.
    int enter = startJump(expr);

    // Compile the block body.
    compile(expr.body(), dest);

    // Complete the catch handler.
    write(expr, OP_EXIT_TRY);

    // Jump over the catch handlers when an exception is not thrown.
    int jumpPastCatch = startJump(expr);

    endJump(enter, OP_ENTER_TRY);

    // TODO(bob): Can we use "dest" here or does it need to be a temp?
    // Write a pseudo-opcode so we know what slot to put the error in.
    write(expr, OP_MOVE, dest);

    compileMatch(expr.catches(), dest, true);

    endJump(jumpPastCatch, OP_JUMP, 1);
  }

  void ExprCompiler::visit(CharacterExpr& expr, int dest)
  {
    // TODO(bob): Putting characters in the constant table is overkill for most
    // characters. Should have an inline opcode for at least basic ASCII or BMP
    // ones.
    int index = chunk_->addConstant(new CharacterObject(expr.value()));
    write(expr, OP_CONSTANT, index, dest);
  }

  void ExprCompiler::visit(DefExpr& expr, int dest)
  {
    gc<String> signature = SignatureBuilder::build(expr);
    int multimethod = compiler_.findMethod(signature);
    methodId method = compiler_.addMethod(new Method(module_, &expr));

    write(expr, OP_METHOD, multimethod, method);
  }

  void ExprCompiler::visit(DefClassExpr& expr, int dest)
  {
    // Evaluate the superclasses.
    int firstSuperclass = getNextTemp();
    for (int i = 0; i < expr.superclasses().count(); i++)
    {
      int superclass = makeTemp();
      compile(expr.superclasses()[i], superclass);
    }

    // Create the class.
    symbolId name = compiler_.addSymbol(expr.name());
    write(expr, OP_CLASS, name, expr.fields().count(), dest);
    write(expr, OP_MOVE, firstSuperclass, expr.superclasses().count());

    releaseTemps(expr.superclasses().count());

    // Also store it in its named variable.
    compileAssignment(expr.pos(), expr.resolved(), dest, true);

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

  void ExprCompiler::visit(FloatExpr& expr, int dest)
  {
    int index = chunk_->addConstant(new FloatObject(expr.value()));
    write(expr, OP_CONSTANT, index, dest);
  }

  void ExprCompiler::visit(FnExpr& expr, int dest)
  {
    // TODO(bob): Handle closures.
    ExprCompiler compiler(compiler_);
    gc<Chunk> chunk = compiler.compile(module_, expr);
    int index = chunk_->addChunk(chunk);

    write(expr, OP_FUNCTION, index, dest);

    compileClosures(expr.pos(), expr.resolved());
  }

  void ExprCompiler::visit(ForExpr& expr, int dest)
  {
    // TODO(bob): Hackish. An actual intermediate representation would help
    // here.
    int iterateMethod = compiler_.findMethod(String::create("0:iterate"));
    ASSERT(iterateMethod != -1, "Should have 'iterate' method in core.");

    int advanceMethod = compiler_.findMethod(String::create("0:advance"));
    ASSERT(advanceMethod != -1, "Should have 'advance' method in core.");

    // Evaluate the iteratable expression.
    int iterator = makeTemp();
    compile(expr.iterator(), iterator);

    // Then call "iterate" on it to get an iterator.
    // TODO(bob): Hackish. An actual intermediate representation would help
    // here.
    write(expr, OP_CALL, iterateMethod, iterator, iterator);

    int loopStart = startJumpBack();

    // Call "advance" on the iterator.
    write(expr, OP_CALL, advanceMethod, iterator, dest);

    // If done, jump to exit.
    int doneSlot = makeTemp();
    write(expr, OP_ATOM, ATOM_DONE, doneSlot);
    write(expr, OP_EQUAL, dest, doneSlot, doneSlot);

    int loopExit = startJump(expr);
    releaseTemp(); // doneSlot.

    // Match on the loop pattern.
    compile(expr.pattern(), dest);

    // Compile the body.
    Loop loop(this);
    compile(expr.body(), dest);

    endJumpBack(expr, loopStart);
    endJump(loopExit, OP_JUMP_IF_TRUE, doneSlot);
    loop.end();

    releaseTemp(); // iterator.

    // TODO(bob): Need to figure out what the result value should be.
  }

  void ExprCompiler::visit(GetFieldExpr& expr, int dest)
  {
    write(expr, OP_GET_CLASS_FIELD, expr.index());
  }

  void ExprCompiler::visit(IfExpr& expr, int dest)
  {
    // Compile the condition.
    compile(expr.condition(), dest);

    // Leave a space for the test and jump instruction.
    int jumpToElse = startJump(expr);

    // Compile the then arm.
    compile(expr.thenArm(), dest);

    // Leave a space for the then arm to jump over the else arm.
    int jumpPastElse = startJump(expr);

    // Compile the else arm.
    endJump(jumpToElse, OP_JUMP_IF_FALSE, dest);

    if (!expr.elseArm().isNull())
    {
      compile(expr.elseArm(), dest);
    }
    else
    {
      // A missing 'else' arm is implicitly 'nothing'.
      write(expr, OP_ATOM, ATOM_NOTHING, dest);
    }

    endJump(jumpPastElse, OP_JUMP, 1);
  }

  void ExprCompiler::visit(ImportExpr& expr, int dest)
  {
    // Nothing to do.
    // TODO(bob): Right now, we treat all imports as happening before any code
    // in the module. In other words, you can refer to an imported name before
    // the actual import expression has been "executed". Should we allow that?
  }

  void ExprCompiler::visit(IntExpr& expr, int dest)
  {
    int index = chunk_->addConstant(new IntObject(expr.value()));
    write(expr, OP_CONSTANT, index, dest);
  }

  void ExprCompiler::visit(IsExpr& expr, int dest)
  {
    compile(expr.value(), dest);

    int type = makeTemp();
    compile(expr.type(), type);

    write(expr, OP_IS, dest, type, dest);

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

    write(expr, OP_LIST, firstElement, expr.elements().count(), dest);

    releaseTemps(expr.elements().count());
  }

  void ExprCompiler::visit(MatchExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    compileMatch(expr.cases(), dest, false);
  }

  void ExprCompiler::visit(NameExpr& expr, int dest)
  {
    ASSERT(expr.resolved()->isResolved(),
           "Names should be resolved before compiling.");

    switch (expr.resolved()->scope())
    {
      case NAME_LOCAL:
        write(expr, OP_MOVE, expr.resolved()->index(), dest);
        break;

      case NAME_CLOSURE:
        write(expr, OP_GET_UPVAR, expr.resolved()->index(), dest);
        break;

      case NAME_MODULE:
        write(expr, OP_GET_VAR, expr.resolved()->module(),
              expr.resolved()->index(), dest);
        break;
    }
  }

  void ExprCompiler::visit(NativeExpr& expr, int dest)
  {
    write(expr, OP_NATIVE, expr.index(), 0, dest);
  }

  void ExprCompiler::visit(NotExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    write(expr, OP_NOT, dest);
  }

  void ExprCompiler::visit(OrExpr& expr, int dest)
  {
    compile(expr.left(), dest);

    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump(expr);

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
    write(expr, OP_RECORD, firstField, type, dest);

    releaseTemps(expr.fields().count());
  }

  void ExprCompiler::visit(ReturnExpr& expr, int dest)
  {
    // Compile the return value.
    if (expr.value().isNull())
    {
      // No value, so implicitly "nothing".
      write(expr, OP_ATOM, ATOM_NOTHING, dest);
    }
    else
    {
      compile(expr.value(), dest);
    }

    write(expr, OP_RETURN, dest);
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
    write(expr, OP_SET_CLASS_FIELD, expr.index());
  }

  void ExprCompiler::visit(StringExpr& expr, int dest)
  {
    int index = chunk_->addConstant(new StringObject(expr.value()));
    write(expr, OP_CONSTANT, index, dest);
  }

  void ExprCompiler::visit(ThrowExpr& expr, int dest)
  {
    // Compile the error object.
    compile(expr.value(), dest);

    // Throw it.
    write(expr, OP_THROW, dest);
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
    int loopExit = startJump(expr);
    releaseTemp(); // condition

    Loop loop(this);
    compile(expr.body(), dest);

    endJumpBack(expr, loopStart);
    endJump(loopExit, OP_JUMP_IF_FALSE, condition);
    loop.end();
  }

  void ExprCompiler::visit(CallLValue& lvalue, int value)
  {
    // TODO(bob): Is overwriting the value slot correct here?
    compileCall(*lvalue.call(), value, value);
  }

  void ExprCompiler::visit(NameLValue& lvalue, int value)
  {
    compileAssignment(lvalue.pos(), lvalue.resolved(), value, false);
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

      write(lvalue.fields()[i].value->pos(), OP_GET_FIELD,
            value, symbol, field);

      // Recurse into the record, using that field.
      lvalue.fields()[i].value->accept(*this, field);

      releaseTemp(); // field.
    }
  }

  void ExprCompiler::visit(WildcardLValue& lvalue, int value)
  {
    // Nothing to do.
  }

  void ExprCompiler::compileMatch(const Array<MatchClause>& clauses, int dest,
                                  bool isCatch)
  {
    Array<int> endJumps;

    // Compile each case.
    for (int i = 0; i < clauses.count(); i++)
    {
      const MatchClause& clause = clauses[i];

      // The last clause in a match expression will throw. All others just jump
      // to the next clause (or to the rethrow if the last clause in a catch).
      bool jumpOnFailure = isCatch || (i != clauses.count() - 1);

      // Compile the pattern (if there is one and it isn't the "else" case).
      PatternCompiler compiler(*this, jumpOnFailure);
      if (!clause.pattern().isNull())
      {
        clause.pattern()->accept(compiler, dest);
      }

      // Compile the body if the match succeeds.
      compile(clause.body(), dest);

      // Then jump past the other cases.
      if (jumpOnFailure)
      {
        endJumps.add(startJump(*clause.body()));

        // If this pattern fails, make it jump to the next case.
        compiler.endJumps();
      }
    }

    // If no clauses in a catch match, then we rethrow it.
    if (isCatch)
    {
      // TODO(bob): Should be a rethrow to preserve the original stack trace.
      write(*clauses[-1].body(), OP_THROW, dest);
    }

    // Patch all the jumps now that we know where the end is.
    for (int i = 0; i < endJumps.count(); i++)
    {
      endJump(endJumps[i], OP_JUMP, 1);
    }
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
      write(call, OP_MOVE, valueSlot, valueArg);
    }

    write(call, OP_CALL, call.resolved(), firstArg, dest);

    if (valueSlot != -1) releaseTemp(); // valueArg.
    releaseTemps(numTemps);
  }

  void ExprCompiler::compileAssignment(gc<SourcePos> pos,
                                       gc<ResolvedName> resolved, int value,
                                       bool isCreate)
  {
    ASSERT(resolved->isResolved(), "Must resolve before compiling.");

    switch (resolved->scope())
    {
      case NAME_LOCAL:
        // Copy the value into the new variable.
        write(pos, OP_MOVE, value, resolved->index());
        break;

      case NAME_CLOSURE:
        write(pos, OP_SET_UPVAR, resolved->index(), value, isCreate ? 1 : 0);
        break;

      case NAME_MODULE:
        // Assign to the top-level variable.
        write(pos, OP_SET_VAR, resolved->module(), resolved->index(), value);
        break;
    }
  }

  void ExprCompiler::compileClosures(gc<SourcePos> pos,
                                     ResolvedProcedure& procedure)
  {
    // When a procedure is created, it needs to capture references to any
    // variables declared outside of itself that it (or one of its nested
    // procedures) accesses. We do this by compiling a series of
    // pseudo-instructions. Each one describes which upvar from the outer
    // procedure should be captured.
    for (int i = 0; i < procedure.closures().count(); i++)
    {
      int index = procedure.closures()[i];
      if (index == -1)
      {
        // This closure is a new one in this function so there's nothing to
        // Capture. So just add a "do nothing" pseudo-op.
        write(pos, OP_GET_UPVAR, 0, 0, 0);
      }
      else
      {
        // Capture the upvar from the outer scope.
        write(pos, OP_GET_UPVAR, index, 0, 1);
      }
    }
  }

  void ExprCompiler::write(const Expr& expr, OpCode op, int a, int b, int c)
  {
    write(expr.pos(), op, a, b, c);
  }

  void ExprCompiler::write(gc<SourcePos> pos, OpCode op, int a, int b, int c)
  {
    write(pos->startLine(), op, a, b, c);
  }

  void ExprCompiler::write(int line, OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);

    chunk_->write(currentFile_, line, MAKE_ABC(a, b, c, op));
  }

  int ExprCompiler::startJump(const Expr& expr)
  {
    return startJump(expr.pos());
  }

  int ExprCompiler::startJump(gc<SourcePos> pos)
  {
    // Just write a dummy op to leave a space for the jump instruction.
    write(pos->startLine(), OP_MOVE);
    return chunk_->count() - 1;
  }

  int ExprCompiler::startJumpBack()
  {
    // Remember the current instruction so we can calculate the offset.
    return chunk_->count() - 1;
  }

  void ExprCompiler::endJump(int from, OpCode op, int a, int b)
  {
    int c;
    int offset = chunk_->count() - from - 1;

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

    chunk_->rewrite(from, MAKE_ABC(a, b, c, op));
  }

  void ExprCompiler::endJumpBack(const Expr& expr, int to)
  {
    int offset = chunk_->count() - to;
    write(expr, OP_JUMP, 0, offset);
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

  Loop::Loop(ExprCompiler* compiler)
  : compiler_(compiler)
  {
    parent_ = compiler_->currentLoop_;
    compiler_->currentLoop_ = this;
  }

  Loop::~Loop()
  {
    ASSERT(compiler_ == NULL, "Forgot to end() loop.");
  }

  void Loop::addBreak(const Expr& expr)
  {
    breaks_.add(compiler_->startJump(expr));
  }

  void Loop::end()
  {
    for (int i = 0; i < breaks_.count(); i++)
    {
      compiler_->endJump(breaks_[i], OP_JUMP, 1);
    }

    compiler_->currentLoop_ = parent_;
    compiler_ = NULL;
  }

  void PatternCompiler::compile(gc<Pattern> pattern, int slot)
  {
    if (pattern.isNull()) return;

    pattern->accept(*this, slot);
  }

  void PatternCompiler::endJumps()
  {
    ASSERT(jumpOnFailure_,
           "Should not generate jumps if the last clause throws.");

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
      const PatternField& field = pattern.fields()[i];

      // Test and destructure the field. This takes two instructions to encode
      // all of the operands.
      int fieldSlot = compiler_.makeTemp();
      int symbol = compiler_.compiler_.addSymbol(field.name);

      if (jumpOnFailure_)
      {
        compiler_.write(pattern.pos(), OP_TEST_FIELD, value, symbol, fieldSlot);
        tests_.add(MatchTest(compiler_.chunk_->count(), -1));
        compiler_.startJump(pattern.pos());
      }
      else
      {
        compiler_.write(pattern.pos(), OP_GET_FIELD, value, symbol, fieldSlot);
      }

      // Recurse into the pattern, using that field.
      field.value->accept(*this, fieldSlot);

      compiler_.releaseTemp();
    }
  }

  void PatternCompiler::visit(TypePattern& pattern, int value)
  {
    // Evaluate the expected type.
    int expected = compiler_.makeTemp();
    compiler_.compile(pattern.type(), expected);

    // Test if the value matches the expected type.
    compiler_.write(pattern.pos(), OP_IS, value, expected, expected);
    writeTest(pattern, expected);

    compiler_.releaseTemp();
  }

  void PatternCompiler::visit(ValuePattern& pattern, int value)
  {
    // Evaluate the expected value.
    int expected = compiler_.makeTemp();
    compiler_.compile(pattern.value(), expected);

    // Test if the value matches the expected one.
    compiler_.write(pattern.pos(), OP_EQUAL, value, expected, expected);
    writeTest(pattern, expected);

    compiler_.releaseTemp();
  }

  void PatternCompiler::visit(VariablePattern& pattern, int value)
  {
    // Assign to the variable if it isn't a throwaway.
    if (*pattern.name() != "_")
    {
      compiler_.compileAssignment(pattern.pos(), pattern.resolved(), value,
                                  true);
    }

    // Compile the inner pattern.
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, value);
    }
  }

  void PatternCompiler::writeTest(const Pattern& pattern, int slot)
  {
    if (jumpOnFailure_)
    {
      tests_.add(MatchTest(compiler_.chunk_->code().count(), slot));
    }

    // Regardless of the action, write the test op. If we are jumping, this
    // will be replaced by a jump.
    compiler_.write(pattern.pos(), OP_TEST_MATCH, slot);
  }
}
