package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.interpreter.builtin.*;
import com.stuffwithstuff.magpie.parser.ExprParser;
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
    ClassObj classClass = new ClassObj(null, "ClassClass");

    // The class of all class objects. Given a class Foo, it's class will be a
    // singleton instance of its metaclass FooClass. The class of FooClass will
    // in turn be this class, Class. FooClass will also *inherit* from Class, so
    // that all of the methods defined on Class are available in FooClass (i.e.
    // "name", "parent", etc.)
    // TODO(bob): This isn't right. ClassClass should be the class of Class, not
    // it's parent. This means that Object new("foo") and Class new("foo") are
    // indistinguishable.
    mClass = new ClassObj(null, "Class");
    mClass.getMixins().add(classClass);
    mGlobalScope.define("Class", mClass);
    
    // Object is the root class of all objects. All parent chains eventually
    // end here. Note that there is no distinct metaclass for class Object: its
    // metaclass is the main metaclass Class.
    mObjectClass = new ClassObj(mClass, "Object");
    mGlobalScope.define("Object", mObjectClass);

    // Add a constructor so you can create new Objects.
    mObjectClass.getMembers().defineMethod(Name.NEW,
        new ClassConstruct(mObjectClass));

    // Now that ClassClass, Class and Object exist, wire them up.
    classClass.bindClass(mClass);
    mClass.getMixins().add(mObjectClass);

    mArrayClass = createGlobalClass("Array");
    
    mBoolClass = createGlobalClass("Bool");
    mTrue = instantiate(mBoolClass, true);
    mFalse = instantiate(mBoolClass, false);
    
    mDynamicClass = createGlobalClass("Dynamic");
    mFnClass = createGlobalClass("Function");
    mIntClass = createGlobalClass("Int");
    mMagpieParserClass = createGlobalClass("MagpieParser");
    mRecordClass = createGlobalClass("Record");
    mRuntimeClass = createGlobalClass("Runtime");
    mStringClass = createGlobalClass("String");
    mTupleClass = createGlobalClass("Tuple");
    mTupleClass.getMembers().defineGetter("count",
        new FieldGetter("count", Expr.name("Int")));
    
    mNothingClass = createGlobalClass("Nothing");
    mNothing = instantiate(mNothingClass, null);

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

    ClassObj reflectClass = createGlobalClass("Reflect");
    
    // Register the built-in methods.
    BuiltIns.registerClass(ArrayBuiltIns.class, mArrayClass);
    BuiltIns.registerClass(BoolBuiltIns.class, mBoolClass);
    BuiltIns.registerClass(ClassBuiltIns.class, mClass);
    BuiltIns.registerClass(FunctionBuiltIns.class, mFnClass);
    BuiltIns.registerClass(IntBuiltIns.class, mIntClass);
    BuiltIns.registerClass(MagpieParserBuiltIns.class, mMagpieParserClass);
    BuiltIns.registerClass(ObjectBuiltIns.class, mObjectClass);
    BuiltIns.registerClass(RecordBuiltIns.class, mRecordClass);
    BuiltIns.registerClass(ReflectBuiltIns.class, reflectClass);
    BuiltIns.registerClass(RuntimeBuiltIns.class, mRuntimeClass);
    BuiltIns.registerClass(StringBuiltIns.class, mStringClass);
    BuiltIns.registerFunctions(BuiltInFunctions.class, this);
    
    EnvironmentBuilder.initialize(this);
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
  
  public Obj evaluateFunctionType(FunctionType type, EvalContext context) {
    if (context == null) {
      context = createTopLevelContext();
    }
    
    // TODO(bob): Do we need to track type parameters here?
    
    Obj paramType = evaluate(type.getParamType(), context);
    Obj returnType = evaluate(type.getReturnType(), context);
    
    // Create a FunctionType object.
    Obj result = invokeMethod(mFnClass, Name.NEW_TYPE,
        createTuple(paramType, returnType));
    
    return result;
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
    
    return invokeMethod(classObj, "new", arg);
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
    mainFn.invoke(this, mNothing, null, mNothing);
  }
  
  public Obj getMember(Position position, Obj receiver, String name) {
    Expect.notNull(receiver);
    
    // Look for a getter.
    Member member = ClassObj.findMember(null, receiver, name);
    if (member != null) {
      switch (member.getType()) {
      case GETTER:
        return member.getDefinition().invoke(this, receiver, null, mNothing);
        
      case METHOD:
        // Bind it to the receiver.
        return new FnObj(mFnClass, receiver, member.getDefinition());
        
      // TODO(bob): What about setters here?
      }
    }
   
    // Look for a field.
    Obj value = receiver.getField(name);
    if (value != null) return value;
    
    // Hackish. If we're looking for _0 and we haven't found it yet, just
    // return the object. That lets objects act like tuples of arity 1 and
    // allows them where a tuple is expected.
    // TODO(bob): Cleaner solution.
    if (name.equals("_0")) {
      return receiver;
    }
    
    // If all else fails, try finding a matching native Java method on the
    // primitive value.
    // TODO(bob): The bound method refactoring broke this. Unbreak.
    /*
    Obj result = callJavaMethod(receiver, name, arg);
    if (result != null) return result;
    */

    return mNothing;
  }

  public Obj apply(Position position, Obj target, List<Obj> typeArgs, Obj arg) {
    Expect.notNull(target);
    Expect.notNull(arg);
    
    while(true) {
      if (target instanceof FnObj) {
        FnObj function = (FnObj)target;
        return function.invoke(this, typeArgs, arg);
      } else {
        // We have an argument, but the receiver isn't a function, so send it a
        // "call" message instead. We'll in turn try to apply the result of
        // that.
        Obj newTarget = getMember(position, target, Name.CALL);
        
        if (target == newTarget) {
          // If we get here, we're in an infinite regress. Since we can't call
          // the target directly, we're sending it a "call" message, but that's
          // returning the exact same object (most likely 'nothing'), so we
          // aren't making any progress. If that happens, fail.
          return throwError("BadCallError");
        }
        
        // Loop and try to apply the new target.
        target = newTarget;
      }
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
    
    return apply(position, resolved, null, arg);
  }

  public void print(String text) {
    mHost.print(text);
  }
  
  public Obj throwError(String errorClassName) {
    // Look up the error class.
    ClassObj classObj = mGlobalScope.get(errorClassName).asClass();
    throw new ErrorException(instantiate(classObj, null));
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

  public Obj getTrue() { return mTrue; }
  public Obj getFalse() { return mFalse; }

  public ClassObj getArrayClass() { return mArrayClass; }
  public ClassObj getBoolClass() { return mBoolClass; }
  public ClassObj getDynamicClass() { return mDynamicClass; }
  public ClassObj getFunctionClass() { return mFnClass; }
  public ClassObj getIntClass() { return mIntClass; }
  public ClassObj getMetaclass() { return mClass; }
  public ClassObj getMagpieParserClass() { return mMagpieParserClass; }
  public ClassObj getNeverClass() { return mNeverClass; }
  public ClassObj getNothingClass() { return mNothingClass; }
  public ClassObj getObjectClass() { return mObjectClass; }
  public ClassObj getRecordClass() { return mRecordClass; }
  public ClassObj getStringClass() { return mStringClass; }
  public ClassObj getTupleClass() { return mTupleClass; }
  
  public Obj createArray(List<Obj> elements) {
    return instantiate(mArrayClass, elements);
  }
  
  public Obj createBool(boolean value) {
    return value ? mTrue : mFalse;
  }

  public Obj createInt(int value) {
    return instantiate(mIntClass, value);
  }
  
  public Obj createString(String value) {
    return instantiate(mStringClass, value);
  }
  
  public FnObj createFn(FnExpr expr, EvalContext context) {
    return new FnObj(mFnClass, context.getThis(),
        new Function(context.getScope(), expr));
  }
  
  public Obj createTuple(Obj... fields) {
    return createTuple(Arrays.asList(fields));
  }
  
  public Obj createTuple(List<Obj> fields) {
    // A tuple is an object with fields whose names are zero-based numbers.
    Obj tuple = instantiate(mTupleClass, null);
    for (int i = 0; i < fields.size(); i++) {
      String name = "_" + Integer.toString(i);
      tuple.setField(name, fields.get(i));
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
        Obj value = initializer.invoke(this, mNothing, null, mNothing);
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
    return getMember(Position.none(), value, Name.STRING).asString();
  }

  public Obj orTypes(Obj left, Obj right) {
    // Never is omitted.
    if (left == mNeverClass) return right;
    if (right == mNeverClass) return left;
    
    Obj orFunction = getGlobal(Name.OR);
    return apply(Position.none(), orFunction, null, createTuple(left, right));
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
  
  public boolean isKeyword(String word) {
    return mParsewords.containsKey(word) || mKeywords.contains(word);
  }
  
  public Map<String, ExprParser> getParsewords() {
    return mParsewords;
  }
  
  public Set<String> getKeywords() {
    return mKeywords;
  }
  
  public void registerParseword(String keyword, ExprParser parser) {
    mParsewords.put(keyword, parser);
  }
  
  public void registerKeyword(String keyword) {
    mKeywords.add(keyword);
  }
  
  public ClassObj createClass(String name) {
    // Create the metaclass. This will hold shared methods on the class.
    ClassObj metaclass = new ClassObj(mClass, name + "Class");
    metaclass.getMixins().add(mClass);
    
    // Create the class object itself. This will hold the instance methods for
    // objects of the class.
    ClassObj classObj = new ClassObj(metaclass, name);
    classObj.getMixins().add(mObjectClass);
    
    // Add the factory methods.
    Callable construct = new ClassConstruct(classObj);
    metaclass.getMembers().defineMethod(Name.CONSTRUCT, construct);
    // By default, "new" just constructs too.
    metaclass.getMembers().defineMethod(Name.NEW, construct);

    return classObj;
  }

  private ClassObj createGlobalClass(String name) {
    ClassObj classObj = createClass(name);
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
  private final ClassObj mFnClass;
  private final ClassObj mIntClass;
  private final ClassObj mMagpieParserClass;
  private final ClassObj mNothingClass;
  private final ClassObj mNeverClass;
  private final ClassObj mObjectClass;
  private final ClassObj mRecordClass;
  private final ClassObj mRuntimeClass;
  private final ClassObj mStringClass;
  private final ClassObj mTupleClass;
  
  private final Obj mNothing;
  private final Obj mTrue;
  private final Obj mFalse;
  private final Stack<String> mScriptPaths = new Stack<String>();
  private final Map<String, ExprParser> mParsewords =
      new HashMap<String, ExprParser>();
  private final Set<String> mKeywords = new HashSet<String>();
}
