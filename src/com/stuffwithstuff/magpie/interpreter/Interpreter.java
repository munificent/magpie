package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.MagpieHost;
import com.stuffwithstuff.magpie.SourceFile;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.intrinsic.ClassInit;
import com.stuffwithstuff.magpie.intrinsic.FieldGetter;
import com.stuffwithstuff.magpie.intrinsic.FieldSetter;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;

public class Interpreter {
  public Interpreter(MagpieHost host) {
    mHost = host;

    // Bootstrap the base module with the core definitions.
    mBaseModule = new Module("magpie.core", mHost.loadModule("magpie.core"), this);
    
    EnvironmentBuilder builder = new EnvironmentBuilder(this);
    mClass = builder.createClassClass();
    builder.initialize();
    
    Scope scope = mBaseModule.getScope();
    mArrayClass = scope.get("Array").asClass();
    mBoolClass = scope.get("Bool").asClass();
    mFnClass = scope.get("Function").asClass();
    mIntClass = scope.get("Int").asClass();
    mListClass = scope.get("List").asClass();
    mNothingClass = scope.get("Nothing").asClass();
    mRecordClass = scope.get("Record").asClass();
    mStringClass = scope.get("String").asClass();
    
    mTrue = mBaseModule.instantiate(mBoolClass, true);
    mFalse = mBaseModule.instantiate(mBoolClass, false);
    mNothing = mBaseModule.instantiate(mNothingClass, null);
    
    evaluateModule(mBaseModule);
    
    // Now load the syntax module so that quotations and metaprogramming work.
    mSyntaxModule = importModule("magpie.syntax");
  }
  
  public void interpret(SourceFile info) {
    evaluateModule(new Module("", info, this));
  }

  public Obj interpret(Expr expression) {
    return evaluate(expression, mBaseModule, mBaseModule.getScope());
  }
  
  public Obj evaluate(Expr expr, Module module, Scope scope) {
    ExprEvaluator evaluator = new ExprEvaluator(module);
    return evaluator.evaluate(expr, scope);
  }
  
  public String evaluateToString(Obj value) {
    Multimethod multimethod = mBaseModule.getScope().lookUpMultimethod(
        Name.TO_STRING);
    return multimethod.invoke(Name.TO_STRING,
        mBaseModule, value, mNothing).asString();
  }
  
  public Obj invoke(Obj leftArg, String method, Obj rightArg) {
    Multimethod multimethod = mBaseModule.getScope().lookUpMultimethod(method);
    return multimethod.invoke(method,
        mBaseModule, leftArg, rightArg);
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

    Multimethod equals = mBaseModule.getScope().lookUpMultimethod(Name.EQEQ);   
    
    // Bootstrap short-cut. If we haven't defined "==" yet, default to identity.
    if (equals == null) return a == b;
    
    mInObjectsEqual = true;
    Obj result = equals.invoke(Name.EQEQ, mBaseModule, a, b);
    mInObjectsEqual = false;
    
    return result.asBool();
  }
  
  public Module importModule(String name) {
    // TODO(bob): Check for circular references.
    
    // If it's a relative name, fully expand it.
    if (name.startsWith(".")) {
      name = mLoadingModules.peek().getName() + name;
    }
    
    Module module = mModules.get(name);
    
    // Only load it once.
    if (module == null) {
      SourceFile info = mHost.loadModule(name);
      module = new Module(name, info, this);
      mModules.put(name, module);
      
      evaluateModule(module);
    }
    
    return module;
  }
  
  public ErrorException error(String errorClassName, String message) {
    // Look up the error class.
    ClassObj classObj = mBaseModule.getScope().get(errorClassName).asClass();

    // TODO(bob): Hackish.
    Context context = mBaseModule;
    
    // TODO(bob): Putting the message in here as the value is kind of hackish,
    // but it ensures we can display an error message even if we aren't able
    // to evaluate any code (like calling "string" on the error).
    Obj error = context.instantiate(classObj, message);
    
    error.setValue(message);
    
    throw new ErrorException(error);
  }
  
  /**
   * Gets the single value "nothing" of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public ClassObj getArrayClass() { return mArrayClass; }
  public ClassObj getBoolClass() { return mBoolClass; }
  public ClassObj getClassClass() { return mClass; }
  public ClassObj getFnClass() { return mFnClass; }
  public ClassObj getIntClass() { return mIntClass; }
  public ClassObj getListClass() { return mListClass; }
  public ClassObj getRecordClass() { return mRecordClass; }
  public ClassObj getStringClass() { return mStringClass; }
  
  public Module getBaseModule() { return mBaseModule; }
  public Module getSyntaxModule() { return mSyntaxModule; }
  
  public Obj getBool(boolean value) {
    return value ? mTrue : mFalse;
  }
  
  public ClassObj createClass(String name, List<ClassObj> parents,
      Map<String, Field> fields, Scope scope, String doc) {
    
    // Translate the fields.
    Map<String, FieldObj> fieldObjs = new HashMap<String, FieldObj>();
    for (Entry<String, Field> field : fields.entrySet()) {
      Callable initializer = null;
      if (field.getValue().getInitializer() != null) {
        FnExpr fn = Expr.fn(field.getValue().getInitializer(), "");
        initializer = new Function(fn, scope);
      }
      
      FieldObj fieldObj = new FieldObj(initializer,
          field.getValue().getPattern());
      fieldObjs.put(field.getKey(), fieldObj);
    }
    
    // Create the class.
    ClassObj classObj = new ClassObj(mClass, name, parents, fieldObjs, doc);
    
    ClassObj colliding = classObj.checkForCollisions();
    if (colliding != null) {
      error(Name.PARENT_COLLISION_ERROR, "Class \"" + name +
          "\" is trying to inherit from \"" + colliding.getName() +
          "\" more than once.");
    }
    
    // Add the constructor.
    Multimethod init = scope.define(Name.INIT, new ClassInit(classObj, scope));
    classObj.bindInitMultimethod(init);
    
    // Add getters and setters for the fields.
    for (Entry<String, Field> entry : fields.entrySet()) {
      // Getter.
      scope.define(entry.getKey(),
          new FieldGetter(classObj, entry.getKey(), scope));

      // Setter, if the field is mutable ("var" instead of "val").
      if (entry.getValue().isMutable()) {
        scope.define(Name.makeAssigner(entry.getKey()),
            new FieldSetter(classObj, entry.getKey(), entry.getValue(), scope));
      }
    }
    
    return classObj;
  }
    
  public MagpieHost getHost() {
    return mHost;
  }
  
  public Map<String, Multimethod> getMultimethods() {
    return mMultimethods;
  }
  
  public Obj getConstructingObject() { return mConstructing.peek(); }
  
  public Obj constructNewObject(Context context, ClassObj classObj, Obj initArg) {
    Obj newObj = context.instantiate(classObj, null);
    
    mConstructing.push(newObj);

    // Call the init() multimethod.
    initializeNewObject(context, classObj, initArg);

    mConstructing.pop();
    
    return newObj;
  }
  
  public void initializeNewObject(Context context, ClassObj classObj, Obj arg) {
    // Keep track of how many times we reach the canonical initializer so that
    // we can generate an error if an init() call fails to bottom out to it.
    int expected = mInitializingCount++;
    
    Multimethod init = classObj.getInitMethod();
    
    // Note: the receiver for init() is the class itself, not the new instance
    // which is considered to be in a hidden state since it isn't initialized
    // yet.
    init.invoke(Name.INIT, context, classObj, arg);

    // Make sure the canonical initializer was called.
    if (mInitializingCount > expected) {
      // Just decrement it so the error doesn't cascade.
      mInitializingCount--;

      error(Name.INITIALIZATION_ERROR,
          "Instance of class " + classObj.getName() + " was not initialized.");
    }
  }
  
  public void finishInitialization() {
    mInitializingCount--;
  }
  
  private void evaluateModule(Module module) {
    MagpieParser parser = new MagpieParser(module.readSource());
    
    mLoadingModules.push(module);
    try {
      // Copy the base stuff in first.
      if (module != mBaseModule) {
        Scope scope = module.getScope();
        for (String name : mBaseModule.getExportedNames()) {
          scope.importName(name, name, mBaseModule, false);
        }
      }
      
      // Evaluate the module.
      try {
        List<Expr> exprs = parser.parseModule();
        for (Expr expr : exprs) {
          evaluate(expr, module, module.getScope());
        }
      } catch (ParseException e) {
        String message = String.format("Syntax error at %s: %s",
            e.getPosition(), e.getMessage());
        mHost.showSyntaxError(message);
      }
    } finally {
      mLoadingModules.pop();
    }
  }
  
  private final MagpieHost mHost;
  
  private final Map<String, Module> mModules = new HashMap<String, Module>();
  private final Map<String, Multimethod> mMultimethods = new HashMap<String, Multimethod>();
  
  private final ClassObj mClass;
  private final ClassObj mArrayClass;
  private final ClassObj mBoolClass;
  private final ClassObj mFnClass;
  private final ClassObj mIntClass;
  private final ClassObj mListClass;
  private final ClassObj mNothingClass;
  private final ClassObj mRecordClass;
  private final ClassObj mStringClass;
  
  private final Obj mNothing;
  private final Obj mTrue;
  private final Obj mFalse;
  
  private final Stack<Module> mLoadingModules = new Stack<Module>();
  private final Module mBaseModule;
  private final Module mSyntaxModule;
  
  private final Stack<Obj> mConstructing = new Stack<Obj>();
  private int mInitializingCount = 0;

  private boolean mInObjectsEqual = false;
}
