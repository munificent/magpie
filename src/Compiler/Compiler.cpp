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
  Module* Compiler::compileProgram(VM& vm, gc<ModuleAst> moduleAst,
                                  ErrorReporter& reporter)
  {
    Compiler compiler(vm, reporter);
    
    Module* module = vm.createModule();
    
    // TODO(bob): Doing this here is hackish. Need to figure out when a module's
    // imports are resolved.
    module->imports().add(vm.coreModule());
    
    // TODO(bob): Hack temp. Cram the core primitives in the multimethod array
    // so that things have the right indexes. This should go away. Instead, we
    // should walk all modules and add their methods, including core.
    compiler.addMethod(String::create("print 0:"), gc<MethodDef>(), vm.coreModule());
    compiler.addMethod(String::create("0: + 0:"), gc<MethodDef>(), vm.coreModule());
    compiler.addMethod(String::create("0: - 0:"), gc<MethodDef>(), vm.coreModule());
    compiler.addMethod(String::create("0: * 0:"), gc<MethodDef>(), vm.coreModule());
    compiler.addMethod(String::create("0: / 0:"), gc<MethodDef>(), vm.coreModule());
    
    // Group all of the methods together into multimethods. This also
    // effectively forward-declares them, so we know their index.
    for (int i = 0; i < moduleAst->defs().count(); i++)
    {
      // TODO(bob): Handle non-method defs when they exist.
      MethodDef* method = moduleAst->defs()[i]->asMethodDef();
      gc<String> signature = SignatureBuilder::build(*method);
      compiler.addMethod(signature, method, module);
    }
    
    // Compile all of the multimethods.
    for (int i = 0; i < compiler.multimethods_.count(); i++)
    {
      gc<Multimethod> multimethod = compiler.multimethods_[i];
      
      // TODO(bob): Hack temp. Skip primitive multimethods.
      if (multimethod->methods()[0]->def().isNull()) continue;
      
      gc<Method> compiled = compileMultimethod(compiler, *multimethod, reporter);
      vm.methods().define(multimethod->signature(), compiled);
    }
    
    // Wrap the module's imperative code in a shell method and compile it.
    gc<Expr> body = moduleAst->body();
    MethodDef* method = new MethodDef(body->pos(), gc<Pattern>(),
        String::create("<module>"), gc<Pattern>(), body);
    
    module->bindBody(compileMethod(compiler, module, *method, reporter));
    
    // TODO:
    // - Make bytecode for calling a primitive.
    // - Make a multimethod containing a primitive compile to a method that
    //   just does a primitive call instruction.
    // - Patterns for primitives.
    // - Make MethodCompiler take a Multimethod.
    
    return module;
  }

  void Compiler::addMethod(gc<String> signature, gc<MethodDef> method,
                           Module* module)
  {
    // See if we already have a multimethod with that signature.
    int index;
    for (index = 0; index < multimethods_.count(); index++)
    {
      if (multimethods_[index]->signature() == signature) break;
    }
    
    if (index == multimethods_.count())
    {
      // A new multimethod, so add it.
      multimethods_.add(new Multimethod(signature));
    }
    
    multimethods_[index]->addMethod(method, module);
  }
  
  int Compiler::findMethod(gc<String> signature)
  {
    for (int i = 0; i < multimethods_.count(); i++)
    {
      if (multimethods_[i]->signature() == signature) return i;
    }
    
    return -1;
  }

  int Compiler::addSymbol(gc<String> name)
  {
    return vm_.addSymbol(name);
  }

  int Compiler::addRecordType(Array<int>& nameSymbols)
  {
    return vm_.addRecordType(nameSymbols);
  }

  int Compiler::getModuleIndex(Module* module)
  {
    return vm_.getModuleIndex(module);
  }
  
  gc<Method> Compiler::compileMultimethod(Compiler& compiler,
                                          Multimethod& multimethod,
                                          ErrorReporter& reporter)
  {
    ASSERT(multimethod.methods().count() == 1, "Multimethods aren't implemented yet.");
    
    gc<MethodInstance> method = multimethod.methods()[0];
    return MethodCompiler(compiler, method->module()).compile(*method->def());
  }
  
  gc<Method> Compiler::compileMethod(Compiler& compiler, Module* module,
                                     MethodDef& method,
                                     ErrorReporter& reporter)
  {
    return MethodCompiler(compiler, module).compile(method);
  }
  
  void MethodInstance::reach()
  {
    Memory::reach(def_);
  }
  
  void Multimethod::addMethod(gc<MethodDef> method, Module* module)
  {
    methods_.add(new MethodInstance(method, module));
  }
  
  void Multimethod::reach()
  {
    Memory::reach(signature_);
    Memory::reach(methods_);
  }
  
  MethodCompiler::MethodCompiler(Compiler& compiler,
                                 Module* module)
  : ExprVisitor(),
    compiler_(compiler),
    module_(module),
    method_(new Method()),
    code_(),
    numLocals_(0),
    numTemps_(0),
    maxSlots_(0)
  {}

  gc<Method> MethodCompiler::compile(MethodDef& method)
  {
    Resolver::resolve(compiler_, *module_, method);
    
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
  
  void MethodCompiler::compileParam(gc<Pattern> param, int& slot)
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

  void MethodCompiler::compileParamField(gc<Pattern> param, int slot)
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
  
  int MethodCompiler::compileArg(gc<Expr> arg)
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
    
  void MethodCompiler::compile(gc<Expr> expr, int dest)
  {
    expr->accept(*this, dest);
  }
  
  void MethodCompiler::compile(gc<Pattern> pattern, int slot)
  {
    if (pattern.isNull()) return;
    
    PatternCompiler compiler(*this);
    pattern->accept(compiler, slot);
  }
  
  void MethodCompiler::visit(AndExpr& expr, int dest)
  {
    compile(expr.left(), dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    compile(expr.right(), dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_FALSE, dest);
  }
  
  void MethodCompiler::visit(AssignExpr& expr, int dest)
  {
    // Compile the value and also make it the result of the expression.
    compile(expr.value(), dest);
    
    // Now pattern match on the value.
    compile(expr.pattern(), dest);
  }
  
  void MethodCompiler::visit(BinaryOpExpr& expr, int dest)
  {
    int a = compileExpressionOrConstant(expr.left());
    int b = compileExpressionOrConstant(expr.right());

    OpCode op;
    bool negate = false;
    switch (expr.type())
    {
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

  void MethodCompiler::visit(BoolExpr& expr, int dest)
  {
    write(OP_BUILT_IN, expr.value() ? BUILT_IN_TRUE : BUILT_IN_FALSE, dest);
  }

  void MethodCompiler::visit(CallExpr& expr, int dest)
  {
    gc<String> signature = SignatureBuilder::build(expr);

    int method = compiler_.findMethod(signature);
    
    if (method == -1)
    {
      compiler_.reporter().error(expr.pos(),
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
  
  void MethodCompiler::visit(CatchExpr& expr, int dest)
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
    
  void MethodCompiler::visit(DoExpr& expr, int dest)
  {
    compile(expr.body(), dest);
  }

  void MethodCompiler::visit(IfExpr& expr, int dest)
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
  
  void MethodCompiler::visit(IsExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    
    int type = makeTemp();
    compile(expr.type(), type);

    write(OP_IS, dest, type, dest);

    releaseTemp(); // type
  }

  void MethodCompiler::visit(MatchExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    compileMatch(expr.cases(), dest);
  }
  
  void MethodCompiler::visit(NameExpr& expr, int dest)
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
  
  void MethodCompiler::visit(NotExpr& expr, int dest)
  {
    compile(expr.value(), dest);
    write(OP_NOT, dest);
  }
  
  void MethodCompiler::visit(NothingExpr& expr, int dest)
  {
    write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
  }

  void MethodCompiler::visit(NumberExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }
  
  void MethodCompiler::visit(OrExpr& expr, int dest)
  {
    compile(expr.left(), dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    compile(expr.right(), dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_TRUE, dest);
  }
  
  void MethodCompiler::visit(RecordExpr& expr, int dest)
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
  
  void MethodCompiler::visit(ReturnExpr& expr, int dest)
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
  
  void MethodCompiler::visit(SequenceExpr& expr, int dest)
  {
    for (int i = 0; i < expr.expressions().count(); i++)
    {
      // TODO(bob): Could compile all but the last expression with a special
      // sigil dest that means "won't use" and some exprs could check that to
      // omit some unnecessary instructions.
      compile(expr.expressions()[i], dest);
    }
  }

  void MethodCompiler::visit(StringExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }
  
  void MethodCompiler::visit(ThrowExpr& expr, int dest)
  {
    // Compile the error object.
    compile(expr.value(), dest);
    
    // Throw it.
    write(OP_THROW, dest);
  }
  
  void MethodCompiler::visit(VariableExpr& expr, int dest)
  {
    // Compile the value.
    compile(expr.value(), dest);

    // Now pattern match on it.
    compile(expr.pattern(), dest);
  }
  
  void MethodCompiler::visit(WhileExpr& expr, int dest)
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
  
  void MethodCompiler::compileMatch(const Array<MatchClause>& clauses, int dest)
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
      endJump(endJumps[i], OP_JUMP, 1);
    }
  }
  
  int MethodCompiler::compileExpressionOrConstant(gc<Expr> expr)
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

  int MethodCompiler::compileConstant(const NumberExpr& expr)
  {
    return method_->addConstant(new NumberObject(expr.value()));
  }

  int MethodCompiler::compileConstant(const StringExpr& expr)
  {
    return method_->addConstant(new StringObject(expr.value()));
  }

  void MethodCompiler::write(OpCode op, int a, int b, int c)
  {
    ASSERT_INDEX(a, 256);
    ASSERT_INDEX(b, 256);
    ASSERT_INDEX(c, 256);

    code_.add(MAKE_ABC(a, b, c, op));
  }

  int MethodCompiler::startJump()
  {
    // Just write a dummy op to leave a space for the jump instruction.
    write(OP_MOVE);
    return code_.count() - 1;
  }

  int MethodCompiler::startJumpBack()
  {
    // Remember the current instruction so we can calculate the offset.
    return code_.count() - 1;
  }

  void MethodCompiler::endJump(int from, OpCode op, int a, int b)
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

  void MethodCompiler::endJumpBack(int to)
  {
    int offset = code_.count() - to;
    write(OP_JUMP, 0, offset);
  }

  int MethodCompiler::getNextTemp() const
  {
    return numLocals_ + numTemps_;
  }
  
  int MethodCompiler::makeTemp()
  {
    numTemps_++;
    if (maxSlots_ < numLocals_ + numTemps_)
    {
      maxSlots_ = numLocals_ + numTemps_;
    }
    
    return numLocals_ + numTemps_ - 1;
  }

  void MethodCompiler::releaseTemp()
  {
    releaseTemps(1);
  }
  
  void MethodCompiler::releaseTemps(int count)
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