package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.intrinsic.IntrinsicLoader;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Implements the visitor pattern on AST nodes, in order to evaluate
 * expressions. This is the heart of the interpreter and is where Magpie code is
 * actually executed.
 */
public class ExprEvaluator implements ExprVisitor<Obj, Scope> {
  public ExprEvaluator(Context context) {
    mContext = context;
  }

  /**
   * Evaluates the given expression in the given context.
   * @param   expr     The expression to evaluate.
   * @param   context  The context in which to evaluate the expression.
   * @return           The result of evaluating the expression.
   */
  public Obj evaluate(Expr expr, Scope scope) {
    if (expr == null) return null;
    return expr.accept(this, scope);
  }

  @Override
  public Obj visit(AssignExpr expr, Scope scope) {
    Obj value = evaluate(expr.getValue(), scope);

    // Try to assign to a local variable.
    if (scope.assign(expr.getName(), value)) return value;
    
    // TODO(bob): Detect this statically.
    throw mContext.error("NoVariableError",
        "Could not find a variable named \"" + expr.getName() + "\".");
  }

  @Override
  public Obj visit(BoolExpr expr, Scope scope) {
    return mContext.toObj(expr.getValue());
  }

  @Override
  public Obj visit(BreakExpr expr, Scope scope) {
    // Outside of a loop, "break" does nothing.
    if (mLoopDepth > 0) {
      throw new BreakException();
    }
    return mContext.nothing();
  }
  
  @Override
  public Obj visit(CallExpr expr, Scope scope) {
    Multimethod multimethod = scope.lookUpMultimethod(expr.getName());
    if (multimethod == null) {
      throw mContext.error("NoMethodError",
          "Could not find a method named \"" + expr.getName() + "\".");
    }

    Obj arg = evaluate(expr.getArg(), scope);
    return multimethod.invoke(new Context(scope.getModule()), arg);
  }
  
  @Override
  public Obj visit(ClassExpr expr, Scope scope) {
    // Look up the parents.
    List<ClassObj> parents = new ArrayList<ClassObj>();
    for (String parentName : expr.getParents()) {
      parents.add(scope.lookUp(parentName).asClass());
    }
    
    ClassObj classObj = mContext.getInterpreter().createClass(expr.getName(),
        parents, expr.getFields(), scope, expr.getDoc());
    
    scope.define(expr.getName(), classObj);

    return classObj;
  }

  @Override
  public Obj visit(FnExpr expr, Scope scope) {
    return mContext.toFunction(expr, scope);
  }

  @Override
  public Obj visit(ImportExpr expr, Scope scope) {
    // TODO(bob): Eventually the schemes should be host-provided plug-ins.
    if (expr.getScheme() == null) {
      Module module = mContext.getInterpreter().importModule(expr.getModule());
      
      if (expr.getName() == null) {
        // Not a specific name, so import all names.
        String prefix;
        if (expr.getRename() == null) {
          // Just use the unqualified name.
          prefix = "";
        } else if (expr.getRename().equals("_")) {
          // Use the module name as a prefix.
          prefix = module.getName() + ".";
        } else {
          prefix = expr.getRename() + ".";
        }
        
        scope.importAll(mContext, prefix, module);
        
        // TODO(bob): Need to import syntax extensions from imported module
        // into this one once EvalContext knows module.
        //module.importSyntax(mBaseModule);

      } else {
        // Importing just one name.
        String rename;
        if (expr.getRename() == null) {
          // Just use the unqualified name.
          rename = expr.getName();
        } else if (expr.getRename().equals("_")) {
          // Use the module name as a prefix.
          rename = module.getName() + "." + expr.getName();
        } else {
          rename = expr.getRename();
        }
        
        scope.importName(mContext, expr.getName(), rename, module);
      }
    } else if (expr.getScheme().equals("classfile")) {
      if (!IntrinsicLoader.loadClass(expr.getModule(), scope)) {
        // TODO(bob): Throw better error.
        throw mContext.error("Error", "Could not load classfile \"" +
            expr.getModule() + "\".");
      }
    }
    
    return mContext.nothing();
  }

  @Override
  public Obj visit(IntExpr expr, Scope scope) {
    return mContext.toObj(expr.getValue());
  }

  @Override
  public Obj visit(ListExpr expr, Scope scope) {
    // Evaluate the elements.
    List<Obj> elements = new ArrayList<Obj>();
    for (Expr element : expr.getElements()) {
      elements.add(evaluate(element, scope));
    }

    return mContext.toList(elements);
  }
  
  @Override
  public Obj visit(LoopExpr expr, Scope scope) {
    try {
      mLoopDepth++;

      // Loop forever. A "break" expression will throw a BreakException to
      // escape this loop.
      while (true) {
        // Evaluate the body in its own scope.
        scope = scope.push();

        evaluate(expr.getBody(), scope);
      }
    } catch (BreakException ex) {
      // Nothing to do.
    } finally {
      mLoopDepth--;
    }

    // TODO(bob): It would be cool if loops could have "else" clauses and then
    // reliably return a value.
    return mContext.nothing();
  }

  @Override
  public Obj visit(MatchExpr expr, Scope scope) {
    // Push a new context so that a variable declared in the value expression
    // itself disappears after the match, i.e.:
    // match var i = 123
    // ...
    // end
    // i should be gone here
    scope = scope.push();
    
    Obj value = evaluate(expr.getValue(), scope);
    
    // Try each pattern until we get a match.
    Obj result = evaluateCases(value, expr.getCases(), scope);
    if (result != null) return result;
    
    // If we got here, no patterns matched.
    throw mContext.error("NoMatchError", "Could not find a match for \"" +
        mContext.getInterpreter().evaluateToString(value) + "\".");
  }

  @Override
  public Obj visit(MethodExpr expr, Scope scope) {
    Function method = new Function(
        Expr.fn(expr.getPosition(), expr.getDoc(),
            expr.getPattern(), expr.getBody()),
        scope);
    
    scope.define(expr.getName(), method);
    
    return mContext.nothing();
  }

  @Override
  public Obj visit(NameExpr expr, Scope scope) {
    Obj variable = scope.lookUp(expr.getName());
    if (variable != null) return variable;
    
    // TODO(bob): Detect this statically.
    throw mContext.error("NoVariableError",
        "Could not find a variable named \"" + expr.getName() + "\".");
  }

  @Override
  public Obj visit(NothingExpr expr, Scope scope) {
    return mContext.nothing();
  }

  @Override
  public Obj visit(QuoteExpr expr, Scope scope) {
    return JavaToMagpie.convertAndUnquote(mContext, expr.getBody(), scope);
  }

  @Override
  public Obj visit(RecordExpr expr, Scope scope) {
    // Evaluate the fields.
    Map<String, Obj> fields = new HashMap<String, Obj>();
    for (Pair<String, Expr> entry : expr.getFields()) {
      Obj value = evaluate(entry.getValue(), scope);
      fields.put(entry.getKey(), value);
    }

    return mContext.toObj(fields);
  }

  @Override
  public Obj visit(ReturnExpr expr, Scope scope) {
    Obj value = evaluate(expr.getValue(), scope);
    throw new ReturnException(value);
  }

  @Override
  public Obj visit(ScopeExpr expr, Scope scope) {
    try {
      scope = scope.push();
      return evaluate(expr.getBody(), scope);
    } catch (ErrorException err) {
      // See if we can catch it here.
      Obj result = this.evaluateCases(err.getError(), expr.getCatches(), scope);
      if (result != null) return result;

      // Not caught here, so just keep unwinding.
      throw err;
    }
  }

  @Override
  public Obj visit(SequenceExpr expr, Scope scope) {
    // Evaluate all of the expressions and return the last.
    Obj result = null;
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr, scope);
    }

    return result;
  }

  @Override
  public Obj visit(StringExpr expr, Scope scope) {
    return mContext.toObj(expr.getValue());
  }

  @Override
  public Obj visit(ThrowExpr expr, Scope scope) {
    Obj value = evaluate(expr.getValue(), scope);
    throw new ErrorException(value);
  }

  @Override
  public Obj visit(UnquoteExpr expr, Scope scope) {
    throw new UnsupportedOperationException(
        "An unquoted expression cannot be directly evaluated.");
  }

  @Override
  public Obj visit(VarExpr expr, Scope scope) {
    Obj value = evaluate(expr.getValue(), scope);

    PatternBinder.bind(mContext, expr.getPattern(), value, scope);
    return value;
  }

  private Obj evaluateCases(Obj value, List<MatchCase> cases, Scope scope) {
    if (cases == null) return null;
    
    for (MatchCase matchCase : cases) {
      Pattern pattern = matchCase.getPattern();
      if (PatternTester.test(mContext, pattern, value, scope)) {
        // Matched. Bind variables and evaluate the body.
        scope = scope.push();
        PatternBinder.bind(mContext, pattern, value,
            scope);
        
        return evaluate(matchCase.getBody(), scope);
      }
    }
    
    return null;
  }

  private final Context mContext;
  private int mLoopDepth = 0;
}