package com.stuffwithstuff.magpie.interpreter;

import java.util.*;

import com.stuffwithstuff.magpie.Identifiers;
import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.interpreter.builtin.*;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;

public class Interpreter {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    // The class hierarchy is a bit confusing in a language where classes are
    // first class and inheritance is supported. Every object has a class.
    // However, classes are objects, which means they also have a class. In
    // addition, a class may also have a parent class.
    //
    // One key piece that it's important to understand is that the methods on
    // an object *always* come from its class. So, when you call a shared
    // method like "Foo bar", you are actually calling an *instance* method on
    // the class of of the object "Foo", its metaclass: FooClass.
    //
    // Given some hypothetical class Foo, here's how it will be wired up with
    // the three core classes:
    //
    // Single line: "inherits from"
    // Double line: "class"
    //
    //                       +------------+
    //                       | ClassClass |
    //                       +------------+
    //                           |   /\
    //                           |   ||
    //                           v   \/
    //    +------------+     +------------+<====+------------+
    //    |    Foo     |---->|   Class    |---->|   Object   |
    //    +------------+     +------------+     +------------+
    //          ||               ^   /\
    //           \\              |   ||
    //            \\         +------------+
    //             \\=======>|  FooClass  |
    //                       +------------+

    
    // Create a top-level scope.
    mGlobalScope = new Scope();

    // The metaclass for Class. Contains the shared methods on Class.
    ClassObj classClass = new ClassObj("ClassClass", null);
    
    // The class of all class objects. Given a class Foo, it's class will be a
    // singleton instance of its metaclass FooClass. The class of FooClass will
    // in turn be this class, Class. FooClass will also *inherit* from Class, so
    // that all of the methods defined on Class are available in FooClass (i.e.
    // "name", "parent", etc.)
    mClass = new ClassObj("Class", classClass);
    mGlobalScope.define("Class", mClass);
    
    // Object is the root class of all objects. All parent chains eventually
    // end here. Note that there is no distinct metaclass for class Object: its
    // metaclass is the main metaclass Class.
    mObjectClass = new ClassObj(mClass, "Object", null);
    mGlobalScope.define("Object", mObjectClass);

    // Add a constructor so you can create new Objects.
    mObjectClass.addMethod(Identifiers.NEW, new ClassNew("Object"));

    // Now that ClassClass, Class and Object exist, wire them up.
    classClass.bindClass(mClass);
    mClass.setParent(mObjectClass);

    mArrayClass = createGlobalClass("Array");
    // TODO(bob): Should really be type and not class, I think?
    //mArrayClass.setParent(mClass);
    
    mBoolClass = createGlobalClass("Bool");
    mDynamicClass = createGlobalClass("Dynamic");
    mExpressionClass = createGlobalClass("Expression");
    mFnClass = createGlobalClass("Function");
    mIntClass = createGlobalClass("Int");
    mRecordClass = createGlobalClass("Record");
    mRuntimeClass = createGlobalClass("Runtime");

    mStringClass = createGlobalClass("String");

    // TODO(bob): At some point, may want different tuple types based on the
    // types of the fields.
    mTupleClass = createGlobalClass("Tuple");
    mTupleClass.defineGetter("count", new FieldGetter("count", Expr.name("Int")));
    // TODO(bob): Hackish.
    for (int i = 0; i < 20; i++) {
      String name = "_" + Integer.toString(i);
      // TODO(bob): Using dynamic as the type here is lame. Ideally, there would
      // be a separate tuple class for each set of tuple field types and it
      // would have field getters that were typed to match the fields.
      mTupleClass.defineGetter(name, new FieldGetter(name, Expr.name("Dynamic")));
    }
    
    mNothingClass = createGlobalClass("Nothing");
    mNothing = mNothingClass.instantiate();

    // "Never" is the evaluated type of an expression that can never yield a
    // result. It's equivalent to the bottom type. More concretely, it's the
    // type of a "return" expression, since a return will always unwind the
    // stack instead of actually yielding a result. In other words, if you do:
    //
    //   foo(return 123)
    //
    // The type of value passed to "foo" is "Never", meaning that in this case,
    // the call to "foo" will never occur since the return will unwind the
    // stack.
    mNeverClass = createGlobalClass("Never");

    // Register the built-in methods.
    BuiltIns.register(ArrayBuiltIns.class, mArrayClass);
    BuiltIns.register(BoolBuiltIns.class, mBoolClass);
    BuiltIns.register(ClassBuiltIns.class, mClass);
    BuiltIns.register(ExpressionBuiltIns.class, mExpressionClass);
    BuiltIns.register(FunctionBuiltIns.class, mFnClass);
    BuiltIns.register(IntBuiltIns.class, mIntClass);
    BuiltIns.register(ObjectBuiltIns.class, mObjectClass);
    BuiltIns.register(RecordBuiltIns.class, mRecordClass);
    BuiltIns.register(RuntimeBuiltIns.class, mRuntimeClass);
    BuiltIns.register(StringBuiltIns.class, mStringClass);
  }
  
  public void load(List<Expr> expressions) {
    EvalContext context = createTopLevelContext();
    
    // Evaluate the expressions. This is the load time evaluation.
    for (Expr expr : expressions) {
      try {
        evaluate(expr, context);
      } catch(ErrorException err) {
        // TODO(bob): Better error message here!
        mHost.runtimeError(expr.getPosition(), "Uncaught error.");
      }
    }
  }

  public String evaluate(Expr expr) {
    EvalContext context = createTopLevelContext();
    Obj result = evaluate(expr, context);
    
    // A null string means "nothing to output".
    if (result == mNothing) return null;
    
    // Convert it to a string.
    return evaluateToString(result);
  }
  
  public Obj evaluateCallableType(Callable callable, boolean justReturnType) {
    // TODO(bob): Hackish.
    // Figure out a context to evaluate the method's type signature in. If it's
    // a user-defined method we'll evaluate it the method's closure so that
    // outer static arguments are available. Otherwise, we'll assume it has no
    // outer scope and just evaluate it in a top-level context.
    EvalContext staticContext;
    boolean isStatic = false;
    Object value = null;
    if (callable instanceof Function) {
      Function function = (Function)callable;
      staticContext = new EvalContext(function.getClosure(), mNothing);
      isStatic = function.getFunction().isStatic();
      value = function.getFunction();
    } else {
      staticContext = createTopLevelContext();
    }
    
    FunctionType type = callable.getType();
    
    if (justReturnType) {
      return evaluate(type.getReturnType(), staticContext);
    } else {
      Obj paramType = evaluate(type.getParamType(), staticContext);
      
      // If it's a static function like foo[T Int -> T], then bind the
      // constraint to the parameter name(s) so that it can be used in the
      // return type.
      if (isStatic) {
        staticContext = staticContext.pushScope();
        staticContext.bind(this, type.getParamNames(), paramType);
      }
      
      Obj returnType = evaluate(type.getReturnType(), staticContext);
      
      // Create a FunctionType object.
      Obj result = invokeMethod(mFnClass, Identifiers.NEW_TYPE,
          createTuple(paramType, returnType, createBool(isStatic)));
      
      // TODO(bob): Hackish. Cram the original function body in there too. That
      // way, if it's a static function, we have access to it during check time.
      result.setValue(value);
      
      return result;
    }
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
    mainFn.invoke(this, mNothing, mNothing);
  }
  
  public Obj getMember(Position position, Obj receiver, String name) {
    // Look for a getter.
    Callable getter = receiver.getClassObj().findGetter(name);
    if (getter != null) {
      return getter.invoke(this, receiver, mNothing);
    }
    
    // Look for a method.
    Callable method = receiver.getClassObj().findMethod(name);
    if (method != null) {
      // Bind it to the receiver.
      return new FnObj(mFnClass, receiver, method);
    }
   
    // Look for a field.
    Obj value = receiver.getField(name);
    if (value != null) return value;
    
    // If all else fails, try finding a matching native Java method on the
    // primitive value.
    // TODO(bob): The bound method refactoring broke this. Unbreak.
    /*
    Obj result = callJavaMethod(receiver, name, arg);
    if (result != null) return result;
    */

    return mNothing;
  }

  public Obj apply(Position position, Obj target, Obj arg) {
    Expect.notNull(target);
    Expect.notNull(arg);
    
    if (target instanceof FnObj) {
      FnObj function = (FnObj)target;
      return function.invoke(this, arg);
    } else {
      // We have an argument, but the receiver isn't a function, so send it a
      // call message instead.
      return invokeMethod(position, target, Identifiers.CALL, arg);
    }
  }

  /**
   * Invokes a named method on an object, passing in the given argument.
   * 
   * @param receiver   The object the method is being invoked on.
   * @param name       The name of the method to invoke.
   * @param arg        The argument passed to the method.
   * @return           The result of invoking the method.
   */
  public Obj invokeMethod(Obj receiver, String name, Obj arg) {
    return invokeMethod(Position.none(), receiver, name, arg);
  }
  
  public Obj invokeMethod(Position position, Obj receiver, String name,
      Obj arg) {
    Expect.notNull(receiver);
    Expect.notNull(arg);
    
    Obj resolved = getMember(position, receiver, name);
    return apply(position, resolved, arg);
  }

  public void print(String text) {
    mHost.print(text);
  }
  
  public Obj throwError(String errorClassName) {
    // Look up the error class.
    ClassObj classObj = (ClassObj) mGlobalScope.get(errorClassName);
    throw new ErrorException(classObj.instantiate());
  }
  
  public void runtimeError(Expr expr, String format, Object... args) {
    mHost.runtimeError(expr.getPosition(), String.format(format, args));
  }
  
  public void runtimeError(String format, Object... args) {
    mHost.runtimeError(Position.none(), String.format(format, args));
  }
  
  public EvalContext createTopLevelContext() {
    return new EvalContext(mGlobalScope, mNothing);
  }
  
  public Scope getGlobals() { return mGlobalScope; }
  
  /**
   * Gets the single value () of type Nothing.
   * @return
   */
  public Obj nothing() { return mNothing; }

  public ClassObj getArrayClass() { return mArrayClass; }
  public ClassObj getBoolClass() { return mBoolClass; }
  public ClassObj getDynamicClass() { return mDynamicClass; }
  public ClassObj getExpressionClass() { return mExpressionClass; }
  public ClassObj getFunctionClass() { return mFnClass; }
  public ClassObj getIntClass() { return mIntClass; }
  public ClassObj getMetaclass() { return mClass; }
  public ClassObj getNeverClass() { return mNeverClass; }
  public ClassObj getNothingClass() { return mNothingClass; }
  public ClassObj getObjectClass() { return mObjectClass; }
  public ClassObj getRecordClass() { return mRecordClass; }
  public ClassObj getStringClass() { return mStringClass; }
  public ClassObj getTupleClass() { return mTupleClass; }
  
  public Obj createArray(List<Obj> elements) {
    return mArrayClass.instantiate(elements);
  }
  
  public Obj createBool(boolean value) {
    return mBoolClass.instantiate(value);
  }

  public Obj createInt(int value) {
    return mIntClass.instantiate(value);
  }
  
  public Obj createString(String value) {
    return mStringClass.instantiate(value);
  }
  
  public FnObj createFn(FnExpr expr, EvalContext context) {
    // Create a new subclass just for this function so that it's implementation
    // of "call" has the correct return and parameter types.
    // TODO(bob): Figure out a simpler way to do this.
    ClassObj fnClass = new ClassObj(mClass, "FunctionClass", mFnClass);
    fnClass.addMethod("call", new FunctionCall(expr.getType()));
    
    return new FnObj(fnClass, context.getThis(),
        new Function(context.getScope(), expr));
  }
  
  public Obj createTuple(Obj... fields) {
    return createTuple(Arrays.asList(fields));
  }
  
  public Obj createTuple(List<Obj> fields) {
    // A tuple is an object with fields whose names are zero-based numbers.
    Obj tuple = mTupleClass.instantiate();
    for (int i = 0; i < fields.size(); i++) {
      String name = "_" + Integer.toString(i);
      tuple.setField(name, fields.get(i));
    }
    
    tuple.setField(Identifiers.COUNT, createInt(fields.size()));
    
    return tuple;
  }
  
  public Obj evaluate(Expr expr, EvalContext context) {
    ExprEvaluator evaluator = new ExprEvaluator(this);
    return evaluator.evaluate(expr, context);
  }
  
  public String evaluateToString(Obj value) {
    return getMember(Position.none(), value, Identifiers.STRING).asString();
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
  
  private ClassObj createGlobalClass(String name) {
    // Create the metaclass. This will hold shared methods on the class.
    ClassObj metaclass = new ClassObj(mClass, name + "Class", mClass);
    
    // Create the class object itself. This will hold the instance methods for
    // objects of the class.
    ClassObj classObj = new ClassObj(metaclass, name, mObjectClass);
    mGlobalScope.define(name, classObj);
    
    return classObj;
  }
  
  // TODO(bob): This is hackish in-progress.
  /* 
  private Obj callJavaMethod(Obj receiver, String name, Obj arg) {
    if (receiver.getValue() != null) {
      Class<?> klass = receiver.getValue().getClass();
      
      // Convert the argument types to their Java equivalents.
      Class<?>[] argClasses;
      Object[] args;
      if ((arg == null) || (arg == mNothing)) {
        argClasses = new Class<?>[0];
        args = new Object[0];
      } else if (arg.getClassObj() == mTupleClass) {
        int count = arg.getField(Identifiers.COUNT).asInt();
        argClasses = new Class<?>[count];
        args = new Object[count];
        for (int i = 0; i < count; i++) {
          Obj field = arg.getTupleField(i);
          argClasses[i] = getJavaClass(field);
          args[i] = field.getValue();
        }
      } else {
        argClasses = new Class<?>[1];
        args = new Object[1];
        argClasses[0] = getJavaClass(arg);
        args[0] = arg.getValue();
      }
      
      try {
        Method javaMethod = klass.getMethod(name, argClasses);
        Object result = javaMethod.invoke(receiver.getValue(), args);
        
        return convertToMagpieObject(result);
      } catch (NoSuchMethodException e) {
        // OK, method not found.
      } catch (IllegalArgumentException e) {
        // OK, no method with the right argument types found.
      } catch (SecurityException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    // Failed to find a method.
    return null;
  }
  
  private Class<?> getJavaClass(Obj object) {
    if (object.getValue() != null) {
      return object.getValue().getClass();
    } else {
      return Object.class;
    }
  }
  
  private Obj convertToMagpieObject(Object value) {
    // Figure out what Magpie class to use.
    ClassObj classObj = mObjectClass;
    if (value != null) {
      if (value.getClass().equals(Boolean.class)) {
        classObj = mBoolClass;
      } else if (value.getClass().equals(Integer.class)) {
        classObj = mIntClass;
      } else if (value.getClass().equals(String.class)) {
        classObj = mStringClass;
      }
    } else {
      classObj = mNothingClass;
    }
    
    return classObj.instantiate(value);
  }
  */
  
  void runtimeError(Position position, String format, Object... args) {
    mHost.runtimeError(position, String.format(format, args));
  }

  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  private final ClassObj mClass;
  private final ClassObj mArrayClass;
  private final ClassObj mBoolClass;
  private final ClassObj mDynamicClass;
  private final ClassObj mExpressionClass;
  private final ClassObj mFnClass;
  private final ClassObj mIntClass;
  private final ClassObj mNothingClass;
  private final ClassObj mNeverClass;
  private final ClassObj mObjectClass;
  private final ClassObj mRecordClass;
  private final ClassObj mRuntimeClass;
  private final ClassObj mStringClass;
  private final ClassObj mTupleClass;
  
  private final Obj mNothing;
  private final Stack<String> mScriptPaths = new Stack<String>();
}
