#include "Ast.h"
#include "Compiler.h"
#include "ErrorReporter.h"
#include "Method.h"
#include "Module.h"
#include "Object.h"
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
      const MethodDef* method = moduleAst->defs()[i]->asMethodDef();
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
      const MethodDef* method = moduleAst->defs()[i]->asMethodDef();
      gc<String> signature = SignatureBuilder::build(*method);
      gc<Method> compiled = compileMethod(vm, module, *method, reporter);
      vm.methods().define(signature, compiled);
    }
    
    // Wrap the body in a shell method and compile it.
    gc<Expr> body = moduleAst->body();
    gc<Pattern> nothing = new ValuePattern(body->pos(),
        new NothingExpr(body->pos()));
    MethodDef* method = new MethodDef(body->pos(),
        nothing, String::create("<module>"), nothing, body);
    
    module->bindBody(compileMethod(vm, module, *method, reporter));
    
    return module;
  }

  gc<Method> Compiler::compileMethod(VM& vm, Module* module,
                                     const MethodDef& method,
                                     ErrorReporter& reporter)
  {
    Compiler compiler(vm, reporter, module);
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
  
  void Compiler::Scope::visit(const RecordPattern& pattern, int unused)
  {
    // Recurse into the fields.
    for (int i = 0; i < pattern.fields().count(); i++)
    {
      pattern.fields()[i].value->accept(*this, unused);
    }
  }
 
  void Compiler::Scope::visit(const TypePattern& pattern, int value)
  {
    // Nothing to do.
  }
  
  void Compiler::Scope::visit(const ValuePattern& pattern, int unused)
  {
    // Nothing to do.
  }
  
  void Compiler::Scope::visit(const VariablePattern& pattern, int unused)
  {
    makeLocal(pattern.pos(), pattern.name());
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, unused);
    }
  }
  
  void Compiler::Scope::visit(const WildcardPattern& pattern, int value)
  {
    // Nothing to do.
  }
  
  Compiler::Compiler(VM& vm, ErrorReporter& reporter, Module* module)
  : ExprVisitor(),
    vm_(vm),
    reporter_(reporter),
    method_(new Method(module)),
    locals_(),
    code_(),
    numTemps_(0),
    maxRegisters_(0),
    scope_(NULL)
  {}

  gc<Method> Compiler::compile(const MethodDef& method)
  {
    // Create a top-level scope.
    Scope scope(this);
    scope_ = &scope;
    
    // Create a fake local for the argument and result value.
    int result = scope.makeLocal(method.pos(), String::create("(return)"));

    // Evaluate the method's parameter patterns.
    if (!method.leftParam().isNull()) reserveVariables(*method.leftParam());
    if (!method.rightParam().isNull()) reserveVariables(*method.rightParam());
    compilePattern(method.leftParam(), result);
    compilePattern(method.rightParam(), result);

    method.body()->accept(*this, result);
    write(OP_RETURN, result);

    method_->setCode(code_, maxRegisters_);
    
    scope.end();
    return method_;
  }
  
  void Compiler::visit(const AndExpr& expr, int dest)
  {
    expr.left()->accept(*this, dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    expr.right()->accept(*this, dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_FALSE, dest);
  }
  
  void Compiler::visit(const BinaryOpExpr& expr, int dest)
  {
    int a = compileExpressionOrConstant(*expr.left());
    int b = compileExpressionOrConstant(*expr.right());

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
    
    if (IS_REGISTER(a)) releaseTemp();
    if (IS_REGISTER(b)) releaseTemp();
  }

  void Compiler::visit(const BoolExpr& expr, int dest)
  {
    write(OP_BUILT_IN, expr.value() ? BUILT_IN_TRUE : BUILT_IN_FALSE, dest);
  }

  void Compiler::visit(const CallExpr& expr, int dest)
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
    
    ASSERT(expr.leftArg().isNull() || expr.rightArg().isNull(),
           "Calls with left and right args aren't implemented yet.");

    // Compile the argument(s).
    // TODO(bob): This is going to need work. Basically, it needs to destructure
    // the left and right arguments to figure out how many actual arguments
    // there are, allocate the right amount of temporaries, compile the args
    // to those, and then call. (For cases where there is just a total of one
    // argument, we can just use the one existing dest register, though.
    // Likewise, the method prelude code needs to handle multiple arguments.
    // For now, since we don't have records, we only support postfix or prefix
    // calls, but not infix. That ensures we only ever need one register.

    if (!expr.leftArg().isNull())
    {
      expr.leftArg()->accept(*this, dest);
    }
    
    if (!expr.rightArg().isNull())
    {
      expr.rightArg()->accept(*this, dest);
    }
    
    write(OP_CALL, method, dest);
  }
  
  void Compiler::visit(const CatchExpr& expr, int dest)
  {
    // Register the catch handler.
    int enter = startJump();
    
    // Compile the block body.
    Scope tryScope(this);
    expr.body()->accept(*this, dest);
    tryScope.end();
    
    // Complete the catch handler.
    write(OP_EXIT_TRY);
    
    // Jump past it if an exception is not thrown.
    int jumpPastCatch = startJump();
    endJump(enter, OP_ENTER_TRY);
    
    // Compile the catch handlers.
    Scope catchScope(this);
    // TODO(bob): Handle multiple catches, compile their patterns, pattern
    // match, etc. For now, just compile the body.
    ASSERT(expr.catches().count() == 1, "Multiple catch clauses not impl.");
    expr.catches()[0].body()->accept(*this, dest);
    catchScope.end();
    
    endJump(jumpPastCatch, OP_JUMP);
  }
    
  void Compiler::visit(const DoExpr& expr, int dest)
  {
    Scope doScope(this);
    expr.body()->accept(*this, dest);
    doScope.end();
  }

  void Compiler::visit(const IfExpr& expr, int dest)
  {
    Scope ifScope(this);

    // Compile the condition.
    expr.condition()->accept(*this, dest);

    // Leave a space for the test and jump instruction.
    int jumpToElse = startJump();

    // Compile the then arm.
    Scope thenScope(this);
    expr.thenArm()->accept(*this, dest);
    thenScope.end();
    
    // Leave a space for the then arm to jump over the else arm.
    int jumpPastElse = startJump();

    // Compile the else arm.
    endJump(jumpToElse, OP_JUMP_IF_FALSE, dest);

    if (!expr.elseArm().isNull())
    {
      Scope elseScope(this);
      expr.elseArm()->accept(*this, dest);
      elseScope.end();
    }
    else
    {
      // A missing 'else' arm is implicitly 'nothing'.
      write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
    }

    endJump(jumpPastElse, OP_JUMP);
    ifScope.end();
  }
  
  void Compiler::visit(const IsExpr& expr, int dest)
  {
    expr.value()->accept(*this, dest);
    
    int type = makeTemp();
    expr.type()->accept(*this, type);

    write(OP_IS, dest, type, dest);

    releaseTemp(); // type
  }
  
  void Compiler::visit(const MatchExpr& expr, int dest)
  {
    // Compile the value.
    expr.value()->accept(*this, dest);
    
    Array<int> endJumps;
    
    // Compile each case.
    for (int i = 0; i < expr.cases().count(); i++)
    {
      const MatchClause& clause = expr.cases()[i];
      bool lastPattern = i == expr.cases().count() - 1;
      
      Scope caseScope(this);
      
      // Reserve the locals up front. This way we'll compile the value to a slot
      // *after* them. This ensures locals always come before temporaries.
      reserveVariables(*clause.pattern());
      
      // Compile the pattern.
      PatternCompiler compiler(*this, !lastPattern);
      clause.pattern()->accept(compiler, dest);
      
      // Compile the body if the match succeeds.
      clause.body()->accept(*this, dest);
      
      // Then jump past the other cases.
      if (!lastPattern)
      {
        endJumps.add(startJump());
        
        // If this pattern fails, make it jump to the next case.
        compiler.endJumps();
      }
      
      caseScope.end();
    }
    
    // Patch all the jumps now that we know where the end is.
    for (int i = 0; i < endJumps.count(); i++)
    {
      endJump(endJumps[i], OP_JUMP);
    }
  }
  
  void Compiler::visit(const NameExpr& expr, int dest)
  {
    // See if it's a local variable.
    int local = locals_.lastIndexOf(expr.name());
    if (local != -1)
    {
      write(OP_MOVE, local, dest);
      return;
    }
    
    // See if it's an imported name. Walk through the modules this one imports.
    // TODO(bob): Need to handle name collisions.
    Module* module = method_->module();
    for (int i = 0; i < module->imports().count(); i++)
    {
      Module* import = module->imports()[i];
      
      // Walk through the names it exports.
      for (int j = 0; j < import->numExports(); j++)
      {
        if (*import->getExportName(j) == *expr.name())
        {
          // Found it.
          write(OP_GET_MODULE, i, j, dest);
          return;
        }
      }
    }
    
    reporter_.error(expr.pos(),
                    "Variable '%s' is not defined.", expr.name()->cString());
  }
  
  void Compiler::visit(const NotExpr& expr, int dest)
  {
    expr.value()->accept(*this, dest);
    write(OP_NOT, dest);
  }
  
  void Compiler::visit(const NothingExpr& expr, int dest)
  {
    write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
  }

  void Compiler::visit(const NumberExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(const OrExpr& expr, int dest)
  {
    expr.left()->accept(*this, dest);
    
    // Leave a space for the test and jump instruction.
    int jumpToEnd = startJump();
    
    expr.right()->accept(*this, dest);
    
    endJump(jumpToEnd, OP_JUMP_IF_TRUE, dest);
  }
  
  void Compiler::visit(const RecordExpr& expr, int dest)
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
      int fieldReg = makeTemp();
      if (i == 0) firstField = fieldReg;
      
      expr.fields()[i].value->accept(*this, fieldReg);
      names.add(vm_.addSymbol(expr.fields()[i].name));
    }
    
    // TODO(bob): Need to sort field names.
    
    // Create the record type.
    int type = vm_.addRecordType(names);
    
    // Create the record.
    write(OP_RECORD, firstField, type, dest);

    for (int i = 0; i < expr.fields().count(); i++)
    {
      releaseTemp();
    }
  }
  
  void Compiler::visit(const ReturnExpr& expr, int dest)
  {
    // Compile the return value.
    if (expr.value().isNull())
    {
      // No value, so implicitly "nothing".
      write(OP_BUILT_IN, BUILT_IN_NOTHING, dest);
    }
    else
    {
      expr.value()->accept(*this, dest);
    }

    write(OP_RETURN, dest);
  }
  
  void Compiler::visit(const SequenceExpr& expr, int dest)
  {
    for (int i = 0; i < expr.expressions().count(); i++)
    {
      // TODO(bob): Could compile all but the last expression with a special
      // sigil dest that means "won't use" and some exprs could check that to
      // omit some unnecessary instructions.
      expr.expressions()[i]->accept(*this, dest);
    }
  }

  void Compiler::visit(const StringExpr& expr, int dest)
  {
    int index = compileConstant(expr);
    write(OP_CONSTANT, index, dest);
  }
  
  void Compiler::visit(const ThrowExpr& expr, int dest)
  {
    // Compile the error object.
    expr.value()->accept(*this, dest);
    
    // Throw it.
    write(OP_THROW, dest);
  }
  
  void Compiler::visit(const VariableExpr& expr, int dest)
  {
    // Reserve the locals up front. This way we'll compile the value to a slot
    // *after* them. This ensures locals always come before temporaries.
    reserveVariables(*expr.pattern());

    // Compile the value.
    // TODO(bob): Why is there a temp for this?
    int valueReg = makeTemp();
    expr.value()->accept(*this, valueReg);

    // TODO(bob): Handle mutable variables.

    // Now pattern match on it.
    compilePattern(expr.pattern(), valueReg);

    // Copy the final result.
    // TODO(bob): Omit this in cases where it won't be used. Most variable
    // declarations are just in sequences.
    write(OP_MOVE, valueReg, dest);

    releaseTemp(); // valueReg.
  }
  
  void Compiler::compilePattern(gc<Pattern> pattern, int dest)
  {
    if (pattern.isNull()) return;
    
    PatternCompiler compiler(*this);
    pattern->accept(compiler, dest);
  }

  int Compiler::compileExpressionOrConstant(const Expr& expr)
  {
    const NumberExpr* number = expr.asNumberExpr();
    if (number != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*number));
    }

    const StringExpr* string = expr.asStringExpr();
    if (string != NULL)
    {
      return MAKE_CONSTANT(compileConstant(*string));
    }

    int dest = makeTemp();

    expr.accept(*this, dest);
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
  
  void PatternCompiler::endJumps()
  {
    // Since this isn't the last case, then every match failure should just
    // jump to the next case.
    for (int j = 0; j < tests_.count(); j++)
    {
      const matchTest& test = tests_[j];
      
      if (test.value == -1)
      {
        // This test is a field destructure, so just set the offset.
        compiler_.endJump(test.position, OP_JUMP); 
      }
      else
      {
        // A normal test.
        compiler_.endJump(test.position, OP_JUMP_IF_FALSE, test.value); 
      }
    }
  }
  
  void PatternCompiler::visit(const RecordPattern& pattern, int value)
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
        tests_.add(matchTest(compiler_.code_.count(), -1));
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
  
  void PatternCompiler::visit(const TypePattern& pattern, int value)
  {
    // Evaluate the expected type.
    int expected = compiler_.makeTemp();
    pattern.type()->accept(compiler_, expected);
    
    // Test if the value matches the expected type.
    compiler_.write(OP_IS, value, expected, expected);
    writeTest(expected);
    
    compiler_.releaseTemp();
  }
  
  void PatternCompiler::visit(const ValuePattern& pattern, int value)
  {
    // Evaluate the expected value.
    int expected = compiler_.makeTemp();
    pattern.value()->accept(compiler_, expected);
    
    // Test if the value matches the expected one.
    compiler_.write(OP_EQUAL, value, expected, expected);
    writeTest(expected);
    
    compiler_.releaseTemp();
  }
  
  void PatternCompiler::visit(const VariablePattern& pattern, int value)
  {
    int variable = compiler_.locals_.lastIndexOf(pattern.name());
    ASSERT(variable != -1, "Should have called declareVariables() already.")
    
    // Copy the value into the new variable.
    compiler_.write(OP_MOVE, value, variable);
    
    // Compile the inner pattern.
    if (!pattern.pattern().isNull())
    {
      pattern.pattern()->accept(*this, value);
    }
  }
  
  void PatternCompiler::visit(const WildcardPattern& pattern, int value)
  {
    // Nothing to do.
  }
  
  void PatternCompiler::writeTest(int expected)
  {
    if (jumpOnFailure_)
    {
      tests_.add(matchTest(compiler_.code_.count(), expected));
    }
    compiler_.write(OP_TEST_MATCH, expected);
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
    const RecordExpr* record = expr->asRecordExpr();
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
    const RecordPattern* record = pattern->asRecordPattern();
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