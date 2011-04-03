package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.interpreter.builtin.*;
import com.stuffwithstuff.magpie.parser.Grammar;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;

public class Interpreter {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    mGrammar = new Grammar();
    
    // Create a top-level scope.
    mGlobalScope = new Scope(host.allowTopLevelRedefinition());
    
    // The class of all classes. Its class is itself.
    mClass = new ClassObj(null, "Class", null);
    mClass.bindClass(mClass);
    mGlobalScope.define("Class", mClass);

    mArrayClass = createGlobalClass("Array");

    mBoolClass = createGlobalClass("Bool");
    mTrue = instantiate(mBoolClass, true);
    mFalse = instantiate(mBoolClass, false);
    
    mFnClass = createGlobalClass("Function");
    mMultimethodClass = createGlobalClass("Multimethod");
    mIntClass = createGlobalClass("Int");
    mRecordClass = createGlobalClass("Record");
    mStringClass = createGlobalClass("String");
    
    mTupleClass = createGlobalClass("Tuple");
    
    mNothingClass = createGlobalClass("Nothing");
    mNothing = instantiate(mNothingClass, null);
    
    // Register the built-in methods.
    /*
    BuiltIns.registerClass(ArrayBuiltIns.class, mArrayClass);
    BuiltIns.registerClass(BoolBuiltIns.class, mBoolClass);
    */
    BuiltIns.register(ClassBuiltIns.class, this);
    /*
    BuiltIns.registerClass(FunctionBuiltIns.class, mFnClass);
    */
    BuiltIns.register(IntBuiltIns.class, this);
    /*
    BuiltIns.registerClass(MultimethodBuiltIns.class, mMultimethodClass);
    BuiltIns.registerClass(ReflectBuiltIns.class, reflectClass);
    BuiltIns.registerClass(RuntimeBuiltIns.class, mRuntimeClass);
     */
    BuiltIns.register(StringBuiltIns.class, this);
    BuiltIns.register(BuiltInFunctions.class, this);
    
    EnvironmentBuilder.initialize(this);
  }
  
  public void interpret(Expr expression) {
    EvalContext context = createTopLevelContext();
    
    evaluate(expression, context);
  }
  
  public Obj evaluate(Expr expr) {
    return evaluate(expr, createTopLevelContext());
  }

  public String evaluateToString(Expr expr) {
    Obj result = evaluate(expr);
    
    // A null string means "nothing to output".
    if (result == mNothing) return null;
    
    // Convert it to a string.
    return evaluateToString(result);
  }

  public Obj construct(String className, Obj... args) {
    Obj classObj = getGlobal(className);
    
    Obj arg;
    if (args.length == 0) {
      arg = mNothing;
    } else if (args.length == 1) {
      arg = args[0];
    } else {
      arg = createTuple(args);
    }
    
    return invoke(classObj, "new", arg);
  }
  
  public Obj getGlobal(String name) {
    return mGlobalScope.get(name);
  }

  public boolean hasMain() {
    return mGlobalScope.get("main") != null;
  }
  
  public void runMain() {
    EvalContext context = createTopLevelContext();
    Obj main = context.lookUp("main");
    if (main == null) return;
    
    if (!(main instanceof FnObj)) {
      throw new InterpreterException("Member \"main\" is not a function.");
    }
    
    Callable mainFn = ((FnObj)main).getCallable();
    mainFn.invoke(this, mNothing);
  }
  
  public Obj invoke(Obj receiver, String method, Obj arg) {
    MultimethodObj multimethod = (MultimethodObj)mGlobalScope.get(method);
    return multimethod.invoke(this, receiver, true, arg);
  }
  
  public boolean objectsEqual(Obj a, Obj b) {
    // Short-cuts to avoid infinite regress. Identical values always match, and
    // "true" and "false" never match each other. This lets us match on values
    // before truthiness or "==" have been bootstrapped.
    if (a == b) return true;

    if (a == mTrue && b == mFalse) return false;
    if (a == mFalse && b == mTrue) return false;

    Obj equals = getGlobal(Name.EQEQ);
    
    // Bootstrap short-cut. If we haven't defined "==" yet, default to identity.
    if (equals == null) return a == b;
    
    Obj result = ((MultimethodObj)equals).invoke(this, null, false, createTuple(a, b));
    
    return result.asBool();
  }
  
  public void print(String text) {
    mHost.print(text);
  }
  
  // TODO(bob): Get rid of this once everything is including a friendly message.
  public ErrorException error(String errorClassName) {
    throw error(errorClassName, "");
  }
  
  public ErrorException error(String errorClassName, String message) {
    // Look up the error class.
    ClassObj classObj = mGlobalScope.get(errorClassName).asClass();

    // TODO(bob): Putting the message in here as the value is kind of hackish,
    // but it ensures we can display an error message even if we aren't able
    // to evaluate any code (like calling "string" on the error).
    Obj error = instantiate(classObj, message);
    
    error.setValue(message);
    
    throw new ErrorException(error);
  }
  
  public EvalContext createTopLevelContext() {
    return new EvalContext(mGlobalScope, null);
  }
  
  public Scope getGlobals() { return mGlobalScope; }
  
  /**
   * Gets the single value () of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public ClassObj getArrayClass() { return mArrayClass; }
  public ClassObj getBoolClass() { return mBoolClass; }
  public ClassObj getFunctionClass() { return mFnClass; }
  public ClassObj getIntClass() { return mIntClass; }
  public ClassObj getClassClass() { return mClass; }
  public ClassObj getMultimethodClass() { return mMultimethodClass; }
  public ClassObj getNothingClass() { return mNothingClass; }
  public ClassObj getRecordClass() { return mRecordClass; }
  public ClassObj getStringClass() { return mStringClass; }
  public ClassObj getTupleClass() { return mTupleClass; }
  
  public void defineMethod(String name, Callable method) {
    Obj existing = mGlobalScope.get(name);
    if (existing != null && !(existing instanceof MultimethodObj)) {
      error("RedefinitionError", "Cannot define a method \"" + name +
          "\" since there is already a variable with that name that is not a multimethod.");
    }
    
    // Create the multimethod if this is the first one.
    MultimethodObj multimethod;
    if (existing != null) {
      multimethod = (MultimethodObj)existing;
    } else {
      multimethod = new MultimethodObj(mMultimethodClass);
      mGlobalScope.define(name, multimethod);
    }
    
    multimethod.addMethod(method);
  }

  public Obj createArray(List<Obj> elements) {
    return instantiate(mArrayClass, elements);
  }
  
  public Obj createBool(boolean value) {
    return value ? mTrue : mFalse;
  }

  public Obj createInt(int value) {
    return instantiate(mIntClass, value);
  }

  public MultimethodObj createMultimethod() {
    return new MultimethodObj(mMultimethodClass);
  }
  
  public Obj createString(String value) {
    return instantiate(mStringClass, value);
  }
  
  public FnObj createFn(FnExpr expr, EvalContext context) {
    return new FnObj(mFnClass,
        new Function(context.getScope(), context.getContainingClass(), expr));
  }
  
  public Obj createTuple(Obj... fields) {
    return createTuple(Arrays.asList(fields));
  }
  
  public Obj createTuple(List<Obj> fields) {
    // A tuple is an object with fields whose names are zero-based numbers.
    Obj tuple = instantiate(mTupleClass, null);
    for (int i = 0; i < fields.size(); i++) {
      tuple.setField(Name.getTupleField(i), fields.get(i));
    }
    
    tuple.setField(Name.COUNT, createInt(fields.size()));
    
    return tuple;
  }
  
  public Obj createRecord(Map<String, Obj> fields) {
    Obj record = instantiate(mRecordClass, null);
    
    for (Entry<String, Obj> field : fields.entrySet()) {
      record.setField(field.getKey(), field.getValue());
    }
    
    return record;
  }
  
  public Obj instantiate(ClassObj classObj, Object primitiveValue) {
    Obj object = new Obj(classObj, primitiveValue);
    
    // Initialize its fields.
    for (Entry<String, Field> field : classObj.getFieldDefinitions().entrySet()) {
      if (field.getValue().hasInitializer()) {
        Callable initializer = field.getValue().getInitializer();
        Obj value = initializer.invoke(this, mNothing);
        object.setField(field.getKey(), value);
      }
    }
    
    return object;
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    ExprEvaluator evaluator = new ExprEvaluator(this);
    return evaluator.evaluate(expr, context);
  }
  
  public String evaluateToString(Obj value) {
    return invoke(value, Name.STRING, null).asString();
  }

  public void pushScriptPath(String path) {
    mScriptPaths.push(path);
  }
  
  public String getCurrentScript() {
    return mScriptPaths.peek();
  }
  
  public void popScriptPath() {
    mScriptPaths.pop();
  }
  
  public MagpieParser createParser(Lexer lexer) {
    return new MagpieParser(lexer, mGrammar);
  }
  
  public Grammar getGrammar() {
    return mGrammar;
  }
  
  public ClassObj createClass(String name, List<ClassObj> parents) {
    // Create the class.
    ClassObj classObj = new ClassObj(mClass, name, parents);
    
    // Add the constructor.
    Callable construct = new ClassConstruct(classObj);
    getMultimethod("new").addMethod(construct);
    
    return classObj;
  }

  public MultimethodObj getMultimethod(String name) {
    Obj obj = mGlobalScope.get(name);
    
    // Define it the first time if not found.
    if (obj == null) {
      obj = new MultimethodObj(mMultimethodClass);
      mGlobalScope.define(name, obj);
    }
    
    return (MultimethodObj)obj;
  }
  
  public ClassObj createGlobalClass(String name, List<ClassObj> parents) {
    ClassObj classObj = createClass(name, parents);
    mGlobalScope.define(name, classObj);
    return classObj;
  }

  public ClassObj createGlobalClass(String name) {
    return createGlobalClass(name, null);
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  private final ClassObj mClass;
  private final ClassObj mArrayClass;
  private final ClassObj mBoolClass;
  private final ClassObj mFnClass;
  private final ClassObj mMultimethodClass;
  private final ClassObj mIntClass;
  private final ClassObj mNothingClass;
  private final ClassObj mRecordClass;
  private final ClassObj mStringClass;
  private final ClassObj mTupleClass;
  
  private final Obj mNothing;
  private final Obj mTrue;
  private final Obj mFalse;
  private final Stack<String> mScriptPaths = new Stack<String>();
  private final Grammar mGrammar;
}
