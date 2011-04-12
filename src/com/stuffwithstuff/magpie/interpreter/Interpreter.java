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
    
    // Create the main module.
    mMainModule = new Module("*main*", host.allowTopLevelRedefinition());
    mModules.put(mMainModule.getName(), mMainModule);
    
    Scope scope = mMainModule.getScope();
    EnvironmentBuilder builder = new EnvironmentBuilder(this, scope);
    mClass = builder.createClassClass();
    builder.initialize();
    
    mBoolClass = scope.get("Bool").asClass();
    mFnClass = scope.get("Function").asClass();
    mIntClass = scope.get("Int").asClass();
    mListClass = scope.get("List").asClass();
    mNothingClass = scope.get("Nothing").asClass();
    mRecordClass = scope.get("Record").asClass();
    mStringClass = scope.get("String").asClass();
    mTupleClass = scope.get("Tuple").asClass();
    
    mTrue = instantiate(mBoolClass, true);
    mFalse = instantiate(mBoolClass, false);
    mNothing = instantiate(mNothingClass, null);
  }
  
  public Obj interpret(Expr expression) {
    return evaluate(expression, new EvalContext(mMainModule.getScope()));
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    ExprEvaluator evaluator = new ExprEvaluator(this);
    return evaluator.evaluate(expr, context);
  }
  
  public String evaluateToString(Obj value) {
    return invoke(value, Name.STRING, null).asString();
  }
  
  public Obj invoke(Obj receiver, String method, Obj arg) {
    Multimethod multimethod = mMainModule.getScope().getMultimethod(method);
    return multimethod.invoke(this, receiver, arg);
  }
  
  public boolean objectsEqual(Obj a, Obj b) {
    // Shortcuts to avoid infinite regress. Identical values always match, and
    // "true" and "false" never match each other. This lets us match on values
    // before truthiness or "==" have been bootstrapped.
    if (a == b) return true;

    if (a == mTrue && b == mFalse) return false;
    if (a == mFalse && b == mTrue) return false;
    
    // Recursion base case. If we're in the middle of dispatching a call to
    // "==", don't call it again, just default to identity.
    if (mInObjectsEqual) return a == b;

    Multimethod equals = mMainModule.getScope().getMultimethod(Name.EQEQ);   
    
    // Bootstrap short-cut. If we haven't defined "==" yet, default to identity.
    if (equals == null) return a == b;
    
    mInObjectsEqual = true;
    Obj result = equals.invoke(this, a, b);
    mInObjectsEqual = false;
    
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
    ClassObj classObj = mMainModule.getScope().get(errorClassName).asClass();

    // TODO(bob): Putting the message in here as the value is kind of hackish,
    // but it ensures we can display an error message even if we aren't able
    // to evaluate any code (like calling "string" on the error).
    Obj error = instantiate(classObj, message);
    
    error.setValue(message);
    
    throw new ErrorException(error);
  }
  
  /**
   * Gets the single value "nothing" of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public ClassObj getClassClass() { return mClass; }
  public ClassObj getBoolClass() { return mBoolClass; }
  public ClassObj getFunctionClass() { return mFnClass; }
  public ClassObj getIntClass() { return mIntClass; }
  public ClassObj getListClass() { return mListClass; }
  public ClassObj getNothingClass() { return mNothingClass; }
  public ClassObj getRecordClass() { return mRecordClass; }
  public ClassObj getStringClass() { return mStringClass; }
  public ClassObj getTupleClass() { return mTupleClass; }

  public Obj createBool(boolean value) {
    return value ? mTrue : mFalse;
  }
  
  public ClassObj createClass(String name, List<ClassObj> parents,
      Map<String, Field> fields, Scope scope) {
    // Create the class.
    ClassObj classObj = new ClassObj(mClass, name, parents, fields, scope);
    
    if (classObj.checkForCollisions()) {
      error("ParentCollisionError");
    }
    
    // Add the constructor.
    Multimethod.define(scope, "init").addMethod(new ClassInit(classObj, scope));
    
    // Add getters and setters for the fields.
    for (Entry<String, Field> entry : fields.entrySet()) {
      // Getter.
      Multimethod getter = Multimethod.define(scope, entry.getKey());
      getter.addMethod(new FieldGetter(classObj, entry.getKey(), scope));

      // Setter, if the field is mutable ("var" instead of "val").
      if (entry.getValue().isMutable()) {
        Multimethod setter = Multimethod.define(scope, entry.getKey() + "_=");
        setter.addMethod(new FieldSetter(classObj,
            entry.getKey(), entry.getValue(), scope));
      }
    }
    
    return classObj;
  }

  public Obj createList(List<Obj> elements) {
    return instantiate(mListClass, elements);
  }

  public Obj createInt(int value) {
    return instantiate(mIntClass, value);
  }

  public Obj createString(String value) {
    return instantiate(mStringClass, value);
  }
  
  public FnObj createFn(FnExpr expr, EvalContext context) {
    return new FnObj(mFnClass, new Function(expr, context.getScope()));
  }
  
  public Obj createTuple(Obj... fields) {
    return createTuple(Arrays.asList(fields));
  }
  
  public Obj createTuple(List<Obj> fields) {
    return instantiate(mTupleClass, fields);
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
      Expr initializer = field.getValue().getInitializer();
      if (initializer != null) {
        Obj value = evaluate(initializer, new EvalContext(classObj.getClosure()));
        object.setField(field.getKey(), value);
      }
    }
    
    return object;
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
  
  public Obj getConstructingObject() { return mConstructing.peek(); }
  
  public Obj constructNewObject(ClassObj classObj, Obj initArg) {
    Obj newObj = instantiate(classObj, null);
    
    mConstructing.push(newObj);
    // Call the init() multimethod.
    initializeNewObject(classObj, initArg);
    mConstructing.pop();
    
    return newObj;
  }
  
  public void initializeNewObject(ClassObj classObj, Obj arg) {
    Multimethod init = Multimethod.define(classObj.getClosure(), "init");
    // Note: the receiver for init() is the class itself, not the new instance
    // which is considered to be in a hidden state since it isn't initialized
    // yet.
    init.invoke(this, classObj, arg);
  }
  
  private final InterpreterHost mHost;
  
  private final Map<String, Module> mModules = new HashMap<String, Module>();
  private final Module mMainModule;
  
  private final ClassObj mClass;
  private final ClassObj mBoolClass;
  private final ClassObj mFnClass;
  private final ClassObj mIntClass;
  private final ClassObj mListClass;
  private final ClassObj mNothingClass;
  private final ClassObj mRecordClass;
  private final ClassObj mStringClass;
  private final ClassObj mTupleClass;
  
  private final Obj mNothing;
  private final Obj mTrue;
  private final Obj mFalse;
  private final Stack<String> mScriptPaths = new Stack<String>();
  private final Grammar mGrammar;
  
  private final Stack<Obj> mConstructing = new Stack<Obj>();

  private boolean mInObjectsEqual = false;
}
