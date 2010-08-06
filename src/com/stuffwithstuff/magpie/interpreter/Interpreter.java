package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.ast.*;

public class Interpreter {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    // Create a top-level scope.
    mGlobalScope = new Scope();
    
    mClassClass = new ClassObj();
    mClassClass.addInstanceMethod("addMethod", new NativeMethod.ClassAddMethod());
    mClassClass.addInstanceMethod("addSharedMethod", new NativeMethod.ClassAddSharedMethod());
    mClassClass.addInstanceMethod("name", new NativeMethod.ClassFieldGetter("name"));
    mClassClass.addInstanceMethod("new", new NativeMethod.ClassNew());

    mBoolClass = new ClassObj(mClassClass);
    mBoolClass.addInstanceMethod("not", new NativeMethod.BoolNot());
    mBoolClass.addInstanceMethod("toString", new NativeMethod.BoolToString());

    mFnClass = new ClassObj(mClassClass);
    
    mIntClass = new ClassObj(mClassClass);
    mIntClass.addInstanceMethod("+", new NativeMethod.IntPlus());
    mIntClass.addInstanceMethod("-", new NativeMethod.IntMinus());
    mIntClass.addInstanceMethod("*", new NativeMethod.IntMultiply());
    mIntClass.addInstanceMethod("/", new NativeMethod.IntDivide());
    mIntClass.addInstanceMethod("toString", new NativeMethod.IntToString());
    mIntClass.addInstanceMethod("==", new NativeMethod.IntEqual());
    mIntClass.addInstanceMethod("!=", new NativeMethod.IntNotEqual());
    mIntClass.addInstanceMethod("<",  new NativeMethod.IntLessThan());
    mIntClass.addInstanceMethod(">",  new NativeMethod.IntGreaterThan());
    mIntClass.addInstanceMethod("<=", new NativeMethod.IntLessThanOrEqual());
    mIntClass.addInstanceMethod(">=", new NativeMethod.IntGreaterThanOrEqual());

    mStringClass = new ClassObj(mClassClass);
    mStringClass.addInstanceMethod("+",     new NativeMethod.StringPlus());
    mStringClass.addInstanceMethod("print", new NativeMethod.StringPrint());

    // TODO(bob): At some point, may want different tuple types based on the
    // types of the fields.
    mTupleClass = new ClassObj(mClassClass);
    mTupleClass.addInstanceMethod("apply", new NativeMethod.TupleGetField());
    mTupleClass.addInstanceMethod("count", new NativeMethod.ClassFieldGetter("count"));
    
    ClassObj nothingClass = new ClassObj(mClassClass);
    mNothing = new Obj(nothingClass);
    
    // Give the classes names and make then available.
    mGlobalScope.define("Bool", mBoolClass);
    mGlobalScope.define("Function", mFnClass);
    mGlobalScope.define("Int", mIntClass);
    mGlobalScope.define("String", mStringClass);
    mGlobalScope.define("Tuple", mTupleClass);

    mBoolClass.setField("name", createString("Bool"));
    mClassClass.setField("name", createString("Class"));
    mFnClass.setField("name", createString("Function"));
    mIntClass.setField("name", createString("Int"));
    nothingClass.setField("name", createString("Nothing"));
    mStringClass.setField("name", createString("String"));
    mTupleClass.setField("name", createString("Tuple"));
  }
  
  public void load(List<Expr> expressions) {
    EvalContext context = EvalContext.topLevel(mGlobalScope, mNothing);
    
    // Evaluate the expressions. This is the load time evaluation.
    for (Expr expr : expressions) {
      evaluate(expr, context);
    }
  }
  
  public List<Integer> analyze() {
    List<Integer> errors = new ArrayList<Integer>();
    // TODO(bob): Type-checking and static analysis goes here.
    return errors;
  }
  
  public void runMain() {
    EvalContext context = EvalContext.topLevel(mGlobalScope, mNothing);
    Obj main = context.lookUp("main");
    if (main == null) return;
    
    if (!(main instanceof Invokable)) {
      throw new InterpreterException("Member \"main\" is not a function.");
    }
    
    FnObj mainFn = (FnObj)main;
    mainFn.invoke(this, mNothing, mNothing);
  }
  
  public void print(String text) {
    mHost.print(text);
  }
  
  public Scope getGlobals() { return mGlobalScope; }
  
  /**
   * Gets the single value () of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public Obj createBool(boolean value) {
    return mBoolClass.instantiate(value);
  }

  public Obj createInt(int value) {
    return mIntClass.instantiate(value);
  }
  
  public Obj createString(String value) {
    return mStringClass.instantiate(value);
  }
  
  public ClassObj createClass() {
    return new ClassObj(mClassClass);
  }
  
  public FnObj createFn(FnExpr expr) {
    return new FnObj(mFnClass, expr);
  }
  
  public Obj createTuple(EvalContext context, Obj... fields) {
    // A tuple is an object with fields whose names are zero-based numbers.
    Obj tuple = mTupleClass.instantiate();
    for (int i = 0; i < fields.length; i++) {
      String name = Integer.toString(i);
      tuple.setField(name, fields[i]);
      tuple.addMethod(name, new NativeMethod.ClassFieldGetter(name));
    }
    
    tuple.setField("count", createInt(fields.length));
    
    return tuple;
  }
    
  public Obj invoke(Obj thisObj, FnExpr function, Obj arg) {
    // Create a new local scope for the function.
    EvalContext context = EvalContext.forMethod(mGlobalScope, thisObj);
    
    // Bind arguments to their parameter names.
    List<String> params = function.getParamNames();
    if (params.size() == 1) {
      context.define(params.get(0), arg);
    } else if (params.size() > 1) {
      // TODO(bob): Hack. Assume the arg is a tuple with the right number of
      // fields.
      for (int i = 0; i < params.size(); i++) {
        context.define(params.get(i), arg.getTupleField(i));
      }
    }
    
    return evaluate(function.getBody(), context);
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    ExprEvaluator evaluator = new ExprEvaluator(this);
    return evaluator.evaluate(expr, context);
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  private final ClassObj mClassClass;
  private final ClassObj mBoolClass;
  private final ClassObj mFnClass;
  private final ClassObj mIntClass;
  private final ClassObj mStringClass;
  private final ClassObj mTupleClass;
  
  private final Obj mNothing;
}
