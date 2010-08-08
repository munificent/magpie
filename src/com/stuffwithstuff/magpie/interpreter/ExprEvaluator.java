package com.stuffwithstuff.magpie.interpreter;

import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.AssignExpr;
import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.BoolExpr;
import com.stuffwithstuff.magpie.ast.CallExpr;
import com.stuffwithstuff.magpie.ast.ClassExpr;
import com.stuffwithstuff.magpie.ast.DefineExpr;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.ExprVisitor;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.IntExpr;
import com.stuffwithstuff.magpie.ast.LoopExpr;
import com.stuffwithstuff.magpie.ast.MethodExpr;
import com.stuffwithstuff.magpie.ast.NameExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;
import com.stuffwithstuff.magpie.ast.Position;
import com.stuffwithstuff.magpie.ast.StringExpr;
import com.stuffwithstuff.magpie.ast.ThisExpr;
import com.stuffwithstuff.magpie.ast.TupleExpr;

public class ExprEvaluator implements ExprVisitor<Obj, EvalContext> {
  public ExprEvaluator(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    mCurrentPosition = expr.getPosition();
    return expr.accept(this, context);
  }

  @Override
  public Obj visit(AssignExpr expr, EvalContext context) {
    if (expr.getTarget() == null) {
      // No target means we're just assigning to a variable (or field of this)
      // with the given name.
      String name = expr.getName();
      Obj value = evaluate(expr.getValue(), context);
      
      // Try to assign to a local.
      if (context.assign(name, value)) return value;
      
      // If not found, try to assign to a member of this.
      Invokable setter = context.getThis().findMethod(name + "=");
      if (setter != null) {
        return setter.invoke(mInterpreter, context.getThis(), value);
      }
      
      throw failure("Couldn't find a variable or member \"%s\" to assign to.", name);
    } else {
      // The target of the assignment is an actual expression, like a.b = c
      Obj target = evaluate(expr.getTarget(), context);
      Obj value = evaluate(expr.getValue(), context);

      // If the assignment statement has an argument and a value, like:
      // a.b c = v (c is the arg, v is the value)
      // then bundle them together:
      if (expr.getTargetArg() != null) {
        Obj targetArg = evaluate(expr.getTargetArg(), context);
        value = mInterpreter.createTuple(context, targetArg, value);
      }

      // Look for a setter method.
      String setterName = expr.getName() + "=";
      Invokable setter = target.findMethod(setterName);
      
      expect(setter != null,
          "Could not find a method named \"%s\" on %s.", setterName, target);
      
      // Invoke the setter.
      return setter.invoke(mInterpreter, target, value);
    }
  }

  @Override
  public Obj visit(BlockExpr expr, EvalContext context) {
    Obj result = null;
    
    // Create a lexical scope.
    EvalContext localContext = context.newBlockScope();
    
    // Evaluate all of the expressions and return the last.
    for (Expr thisExpr : expr.getExpressions()) {
      result = evaluate(thisExpr, localContext);
    }
    
    return result;
  }

  @Override
  public Obj visit(BoolExpr expr, EvalContext context) {
    return mInterpreter.createBool(expr.getValue());
  }

  @Override
  public Obj visit(CallExpr expr, EvalContext context) {
    Obj arg = evaluate(expr.getArg(), context);
    
    // Handle a named target.
    if (expr.getTarget() instanceof NameExpr) {
      String name = ((NameExpr)expr.getTarget()).getName();
      
      // Look for a local variable with the name.
      Obj local = context.lookUp(name);
      if (local != null) {
        expect(local instanceof Invokable,
            "Can not call a local variable that does not contain a function.");
        
        Invokable function = (Invokable)local;
        return function.invoke(mInterpreter, mInterpreter.nothing(), arg);
      }
      
      // Look for an implicit call to a method on this with the name.
      Invokable method = context.getThis().findMethod(name);
      if (method != null) {
        return method.invoke(mInterpreter, context.getThis(), arg);
      }
      
      // Try to call it as a method on the argument. In other words,
      // "abs 123" is equivalent to "123.abs".
      method = arg.findMethod(name);
      expect(method != null,
          "Could not find a method \"%s\" on %s.", name, arg);

      return method.invoke(mInterpreter, arg, mInterpreter.nothing());
    }
    
    // Not an explicit named target, so evaluate it and see if it's callable.
    Obj target = evaluate(expr.getTarget(), context);
    
    expect(target instanceof FnObj,
        "Can not call an expression that does not evaluate to a function.");

    FnObj targetFn = (FnObj)target;
    return targetFn.invoke(mInterpreter, mInterpreter.nothing(), arg);
  }

  @Override
  public Obj visit(ClassExpr expr, EvalContext context) {
    // Look up the class if we are extending one, otherwise create it.
    ClassObj metaclass;
    ClassObj classObj;
    if (expr.isExtend()) {
      // TODO(bob): Should this be a generic expression that returns a class?
      // Like: class foo.bar.bang
      Obj obj = context.lookUp(expr.getName());
      
      expect(obj != null, "Could not find a class object named \"%s\".",
          expr.getName());
      expect(obj instanceof ClassObj, "Object \"%s\" is not a class.",
          expr.getName());
      
      classObj = (ClassObj)obj;
      metaclass = classObj.getClassObj();
    } else {
      // Create a metaclass. This will be the class of the class. It will hold
      // the shared methods off the class. So, if we're defining a class "Foo"
      // with a shared method "bar", we will create a metaclass FooClass. It
      // will have an *instance* method "bar", and there will be one instance
      // of this metaclass: Foo.
      metaclass = mInterpreter.createClass();
      
      // Add the methods every class instance supports.
      metaclass.addInstanceMethod("addMethod", new NativeMethod.ClassAddMethod());
      metaclass.addInstanceMethod("addSharedMethod", new NativeMethod.ClassAddSharedMethod());
      metaclass.addInstanceMethod("name", new NativeMethod.ClassFieldGetter("name",
          new NameExpr("String")));
      metaclass.addInstanceMethod("new", new NativeMethod.ClassNew(expr.getName()));
      
      classObj = new ClassObj(metaclass);
      classObj.setField("name", mInterpreter.createString(expr.getName()));
    }
    
    // Add the constructors.
    for (FnExpr constructorFn : expr.getConstructors()) {
      FnObj fnObj = mInterpreter.createFn(constructorFn);
      classObj.addConstructor(fnObj);
    }
    
    // Evaluate and define the shared fields.
    EvalContext classContext = context.bindThis(classObj);
    for (Entry<String, Expr> field : expr.getSharedFields().entrySet()) {
      Obj value = evaluate(field.getValue(), classContext);
      
      classObj.setField(field.getKey(), value);
      
      // Add a getter.
      metaclass.addInstanceMethod(field.getKey(),
          new NativeMethod.ClassFieldGetter(field.getKey(), null));
      
      // Add a setter.
      metaclass.addInstanceMethod(field.getKey() + "=",
          new NativeMethod.ClassFieldSetter(field.getKey(), null));
    }
    
    // Define the shared methods.
    for (Entry<String, List<FnExpr>> methods : expr.getSharedMethods().entrySet()) {
      for (FnExpr method : methods.getValue()) {
        FnObj methodObj = mInterpreter.createFn(method);
        metaclass.addInstanceMethod(methods.getKey(), methodObj);
      }
    }
    
    // Define the instance methods.
    for (Entry<String, List<FnExpr>> methods : expr.getMethods().entrySet()) {
      for (FnExpr method : methods.getValue()) {
        FnObj methodObj = mInterpreter.createFn(method);
        classObj.addInstanceMethod(methods.getKey(), methodObj);
      }
    }
    
    // Define the getters and setters for the fields.
    for (String field : expr.getFields().keySet()) {
      // Note that we don't provide type expressions for the fields here because
      // we only know the initializer expression. We don't have an actual type
      // annotation.
      
      // Add a getter.
      classObj.addInstanceMethod(field,
          new NativeMethod.ClassFieldGetter(field, null));
      
      // Add a setter.
      classObj.addInstanceMethod(field + "=",
          new NativeMethod.ClassFieldSetter(field, null));
    }
    
    for (Entry<String, Expr> entry : expr.getFieldDeclarations().entrySet()) {
      // Add a getter.
      classObj.addInstanceMethod(entry.getKey(),
          new NativeMethod.ClassFieldGetter(entry.getKey(), entry.getValue()));
      
      // Add a setter.
      classObj.addInstanceMethod(entry.getKey() + "=",
          new NativeMethod.ClassFieldSetter(entry.getKey(), entry.getValue()));
    }
    
    // Add the field initializers to the class so it can evaluate them when an
    // object is constructed.
    classObj.defineFields(expr.getFields());
    
    // TODO(bob): Need to add constructors here...
    
    // Define a variable for the class in the current scope.
    context.define(expr.getName(), classObj);
    return classObj;
  }

  @Override
  public Obj visit(DefineExpr expr, EvalContext context) {
    Obj value = evaluate(expr.getValue(), context);

    context.define(expr.getName(), value);
    return value;
  }

  @Override
  public Obj visit(FnExpr expr, EvalContext context) {
    return mInterpreter.createFn(expr);
  }

  @Override
  public Obj visit(IfExpr expr, EvalContext context) {
    // Evaluate all of the conditions.
    boolean passed = true;
    for (Expr condition : expr.getConditions()) {
      Obj result = evaluate(condition, context);
      if (!((Boolean)result.getPrimitiveValue()).booleanValue()) {
        // Condition failed.
        passed = false;
        break;
      }
    }
    
    // Evaluate the body.
    if (passed) {
      return evaluate(expr.getThen(), context);
    } else {
      return evaluate(expr.getElse(), context);
    }
  }

  @Override
  public Obj visit(IntExpr expr, EvalContext context) {
    return mInterpreter.createInt(expr.getValue());
  }

  @Override
  public Obj visit(LoopExpr expr, EvalContext context) {
    boolean done = false;
    while (true) {
      // Evaluate the conditions.
      for (Expr conditionExpr : expr.getConditions()) {
        // See if the while clause is still true.
        Obj condition = evaluate(conditionExpr, context);
        if (((Boolean)condition.getPrimitiveValue()).booleanValue() != true) {
          done = true;
          break;
        }
      }
      
      // If any clause failed, stop the loop.
      if (done) break;
      
      evaluate(expr.getBody(), context);
    }
    
    // TODO(bob): It would be cool if loops could have "else" clauses and then
    // reliably return a value.
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(MethodExpr expr, EvalContext context) {
    Obj receiver = evaluate(expr.getReceiver(), context);
    Obj arg = evaluate(expr.getArg(), context);
    
    Invokable method = receiver.findMethod(expr.getMethod());
    expect (method != null,
        "Could not find a method named \"%s\" on %s.",
        expr.getMethod(), receiver);

    return method.invoke(mInterpreter, receiver, arg);
  }

  @Override
  public Obj visit(NameExpr expr, EvalContext context) {
    // Look up a named variable.
    Obj variable = context.lookUp(expr.getName());
    if (variable != null) return variable;
    
    Invokable method = context.getThis().findMethod(expr.getName());
    expect (method != null,
        "Could not find a variable named \"%s\".",
        expr.getName());
    
    return method.invoke(mInterpreter, context.getThis(), mInterpreter.nothing());
  }

  @Override
  public Obj visit(NothingExpr expr, EvalContext context) {
    return mInterpreter.nothing();
  }

  @Override
  public Obj visit(StringExpr expr, EvalContext context) {
    return mInterpreter.createString(expr.getValue());
  }

  @Override
  public Obj visit(ThisExpr expr, EvalContext context) {
    return context.getThis();
  }

  @Override
  public Obj visit(TupleExpr expr, EvalContext context) {
    // Evaluate the fields.
    Obj[] fields = new Obj[expr.getFields().size()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = evaluate(expr.getFields().get(i), context);
    }

    return mInterpreter.createTuple(context, fields);
  }
  
  private void expect(boolean condition, String format, Object... args) {
    if (!condition) {
      throw failure(format, args);
    }
  }
  
  /**
   * Returns a new interpreter exception. It should be called like:
   * 
   *    throw failure(...);
   * 
   * Note that this *returns* an exception instead of throwing it so that you
   * can use it in places where the Java compiler is doing reachability
   * analysis. For example, you can do "throw failure(...)" in the last line of
   * a function with a non-void return type and Java will allow it. If fail did
   * the throw internally, it would have no way of knowing the function doesn't
   * return.
   */
  private InterpreterException failure(String format, Object... args) {
    String message = String.format(format, args);
    message = String.format("%s: %s", mCurrentPosition, message);
    return new InterpreterException(message);
  }

  private final Interpreter mInterpreter;
  private Position mCurrentPosition;
}