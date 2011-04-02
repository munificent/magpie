package com.stuffwithstuff.magpie.interpreter;

import java.util.*;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;
import com.stuffwithstuff.magpie.interpreter.builtin.*;
import com.stuffwithstuff.magpie.parser.Grammar;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.Expect;

public class Interpreter {
  public Interpreter(InterpreterHost host) {
    mHost = host;
    
    mGrammar = new Grammar();
    
    // Every object in Magpie has a class. Every class is an object, which
    // means it also has a class (a metaclass). In addition, a class may
    // inherit from one or more parent classes to get additional methods.
    //
    // To figure out what these relationships look like for classes and their
    // metaclasses is a bit tricky. The basic way to answer this is to look at
    // the methods that different objects should support. Those will in turn
    // dictate what classes those objects should be. Given a hypothetical class
    // Foo:
    //
    // class Foo
    //     def hi() print("hi")
    //     shared def hello() print("hello")
    // end
    //
    // Member call               Class where it's defined
    // --------------------------------------------------
    // foo hi()                  Foo
    // foo type                  Object
    // Foo construct()           FooMetaclass
    // Foo hello()               FooMetaclass
    // Foo defineMethod()        Class
    // Class new("Foo")          ClassMetaclass
    //
    // "construct()" should be defined on the class of Foo, FooMetaclass.
    // "hi()" should be defined on the class of foo, Foo.
    // "hello()" should be defined on the class of Foo, FooMetaclass.
    // "new()" (for creating a new class) should be on the class of Class,
    // ClassMetaclass.
    // "type" should be available on all objects, so it should be in Object.
    // "name" should be available on all class objects, so it should be in
    // Class and the class of Foo, FooMetaclass, should inherit it.
    //
    // In diagram form, the class chain is (where the arrow means "has class"):
    //
    // [Foo]----->[FooMetaclass]--------.     .----.
    //                                  v     v    |
    // [Class]--->[ClassMetaclass]--->[Metaclass]--'
    //                                  ^
    // [Object]-->[ObjectMetaclass]-----'
    //
    // The parent classes are:
    //
    // FooMetaclass    --> Class
    // ClassMetaclass  --> Class
    // ObjectMetaclass --> Class
    // Metaclass       --> Class
    //
    // There is one big of "magic" here. A class does not need to inherit from
    // Object to get its methods. Instead, all classes implicitly have access
    // to those methods. That enables us to have a conceptual top type (Object)
    // without having all classes inherit from it. That lets us make our class
    // hierarchy be a strict tree, which simplifies all sorts of stuff.
    //
    // Once we're using multimethods that can be specialized on Any, that magic
    // will mostly go away.
    
    // Create a top-level scope.
    mGlobalScope = new Scope(host.allowTopLevelRedefinition());

    // The special class "Metaclass" is the ultimate metaclass of all
    // metaclasses, including itself.
    mMetaclass = new ClassObj(null, "Metaclass");
    mMetaclass.bindClass(mMetaclass);
    
    // Define the main Object parent class. All classes will inherit this in so
    // that the common methods like "string" are available everywhere.
    ClassObj objectMetaclass = new ClassObj(mMetaclass, "ObjectMetaclass");
    mObjectClass = new ClassObj(objectMetaclass, "Object");
    mGlobalScope.define("Object", mObjectClass);

    // The metaclass for Class. Contains the shared methods on Class, like "new"
    // for creating a new class.
    ClassObj classMetaclass = new ClassObj(mMetaclass, "ClassMetaclass");
    
    // The class that all class objects inherit. Given a class Foo,
    // FooMetaclass will inherit this in so that the methods available on all
    // classes (like "defineMethod") are available on Foo.
    mClass = new ClassObj(classMetaclass, "Class");
    mGlobalScope.define("Class", mClass);

    // Now that we have Class, we can go back and have the metaclasses inherit
    // it.
    mMetaclass.getParents().add(mClass);
    classMetaclass.getParents().add(mClass);
    objectMetaclass.getParents().add(mClass);
    
    mArrayClass = createGlobalClass("Array");
    
    mBoolClass = createGlobalClass("Bool");
    mTrue = instantiate(mBoolClass, true);
    mFalse = instantiate(mBoolClass, false);
    
    mFnClass = createGlobalClass("Function");
    mMultimethodClass = createGlobalClass("Multimethod");
    mIntClass = createGlobalClass("Int");
    mMagpieParserClass = createGlobalClass("MagpieParser");
    mRecordClass = createGlobalClass("Record");
    mRuntimeClass = createGlobalClass("Runtime");
    mStringClass = createGlobalClass("String");
    
    mTupleClass = createGlobalClass("Tuple");
    mTupleClass.getMembers().defineGetter("count", new FieldGetter("count"));

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
    BuiltIns.registerClass(MultimethodBuiltIns.class, mMultimethodClass);
    BuiltIns.registerClass(ObjectBuiltIns.class, mObjectClass);
    BuiltIns.registerClass(ReflectBuiltIns.class, reflectClass);
    BuiltIns.registerClass(RuntimeBuiltIns.class, mRuntimeClass);
    BuiltIns.registerClass(StringBuiltIns.class, mStringClass);
    BuiltIns.registerFunctions(BuiltInFunctions.class, this);
    
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
    mainFn.invoke(this, mNothing, mNothing);
  }
  
  public Obj getQualifiedMember(Position position, Obj receiver, ClassObj containingClass,
      String name) {
    Obj member = lookupMember(position, receiver, containingClass, name);
    if (member != null) return member;
    
    return mNothing;
  }
  
  public Obj getMember(Position position, Obj receiver, ClassObj containingClass, String name,
      Iterable<String> namespaces) {
    Expect.notNull(receiver);
    
    if (Name.isPrivate(name)) {
      // TODO(bob): Hackish.
      String className = "__noClass__";
      if (containingClass != null) className = containingClass.getName();
      name = Name.makeClassPrivate(className, name);
    }
    
    if (!name.contains(".")) {
      // An unqualified name, so walk the used namespaces first.
      for (String namespace : namespaces) {
        Obj object = lookupMember(position, receiver, containingClass,
            namespace + "." + name);
        if (object != null) return object;
      }
    }
    
    // If we got here, it was already qualified, or wasn't in any namespace, so
    // try the global one.
    Obj object = lookupMember(position, receiver, containingClass, name);
    if (object != null) return object;
    
    return mNothing;
  }

  public Obj apply(Position position, Obj target, Obj arg) {
    Expect.notNull(target);
    Expect.notNull(arg);
    
    while(true) {
      if (target instanceof FnObj) {
        FnObj function = (FnObj)target;
        return function.invoke(this, arg);
      } else if (target instanceof MultimethodObj) {
        MultimethodObj method = (MultimethodObj)target;
        return method.invoke(this, mNothing, true, arg);
      } else {
        // We have an argument, but the receiver isn't a function, so send it a
        // "call" message instead. We'll in turn try to apply the result of
        // that.
        Obj newTarget = getQualifiedMember(position, target, null, Name.CALL);
        
        if (target == newTarget) {
          // If we get here, we're in an infinite regress. Since we can't call
          // the target directly, we're sending it a "call" message, but that's
          // returning the exact same object (most likely 'nothing'), so we
          // aren't making any progress. If that happens, fail.
          throw error("BadCallError", position.toString());
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
  public Obj invokeMethod(Obj receiver, String name,
      Obj arg) {
    Expect.notNull(receiver);
    Expect.notNull(arg);
    
    // TODO(bob): Hack temp while moving to multimethods. Look for a
    // multimethod first.
    if (mGlobalScope.get(name) instanceof MultimethodObj) {
      MultimethodObj multimethod = (MultimethodObj)mGlobalScope.get(name);
      return multimethod.invoke(this, receiver, true, arg);
    }
    
    Obj resolved = getQualifiedMember(Position.none(), receiver, null, name);
    
    return apply(Position.none(), resolved, arg);
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
    
    Obj result = apply(Position.none(), equals, createTuple(a, b));
    
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
    return new EvalContext(mGlobalScope, mNothing, null);
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
  public ClassObj getMetaclass() { return mClass; }
  public ClassObj getMagpieParserClass() { return mMagpieParserClass; }
  public ClassObj getMultimethodClass() { return mMultimethodClass; }
  public ClassObj getNeverClass() { return mNeverClass; }
  public ClassObj getNothingClass() { return mNothingClass; }
  public ClassObj getObjectClass() { return mObjectClass; }
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
  
  public Obj createString(String value) {
    return instantiate(mStringClass, value);
  }
  
  public FnObj createFn(FnExpr expr, EvalContext context) {
    return new FnObj(mFnClass, context.getThis(),
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
        Obj value = initializer.invoke(this, mNothing, mNothing);
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
    return getQualifiedMember(Position.none(), value, null, Name.STRING).asString();
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
  
  public ClassObj createClass(String name) {
    // TODO(bob): Since we can do value methods, metaclasses probably aren't
    // needed. To make a "static method", just make a method that takes the
    // class as a value.
    // Create the metaclass. This will hold shared methods on the class.
    ClassObj metaclass = new ClassObj(mClass, name + "Metaclass");
    metaclass.getParents().add(mClass);
    
    // Create the class object itself. This will hold the instance methods for
    // objects of the class.
    ClassObj classObj = new ClassObj(metaclass, name);
    
    // Add the factory methods.
    Callable construct = new ClassConstruct(classObj);
    
    MultimethodObj newMultimethod = getMultimethod("new");
    newMultimethod.addMethod(construct);
    
    // TODO(bob): Now that methods can be overloaded, this can go away...
    MultimethodObj constructMultimethod = getMultimethod("construct");
    constructMultimethod.addMethod(construct);
    
    /*
    metaclass.getMembers().defineMethod(Name.CONSTRUCT, construct);
    // By default, "new" just constructs too.
    metaclass.getMembers().defineMethod(Name.NEW, construct);
    */
    
    return classObj;
  }

  private MultimethodObj getMultimethod(String name) {
    Obj obj = mGlobalScope.get(name);
    
    // Define it the first time if not found.
    if (obj == null) {
      obj = new MultimethodObj(mMultimethodClass);
      mGlobalScope.define(name, obj);
    }
    
    return (MultimethodObj)obj;
  }
  
  public ClassObj createGlobalClass(String name) {
    ClassObj classObj = createClass(name);
    mGlobalScope.define(name, classObj);
    return classObj;
  }
  
  public Member findMember(ClassObj classObj, ClassObj containingClass,
      String name) {
    Member member = classObj.findMember(containingClass, name);
    if (member != null) return member;
    
    // Not found on the class or its parents, so default to Object.
    // TODO(bob): Once we've got multimethods working all of the methods on
    // Object should become methods specialized on Any, and this can go away
    // completely.
    return mObjectClass.findMember(containingClass, name);
  }
  
  private Obj lookupMember(Position position, Obj receiver, ClassObj containingClass,
      String name) {
    Expect.notNull(receiver);
    
    // Look for a getter.
    Member member = findMember(receiver.getClassObj(), containingClass, name);
    if (member != null) {
      switch (member.getType()) {
      case GETTER:
        return member.getDefinition().invoke(this, receiver, mNothing);
        
      case METHOD:
        // Bind it to the receiver.
        return new FnObj(mFnClass, receiver, member.getDefinition());
        
      // TODO(bob): What about setters here?
      }
    }
   
    // Look for a field.
    if (!Name.isPrivate(name)) {
      Obj value = receiver.getField(name);
      if (value != null) return value;
    }
    
    // Hackish. Let objects act like tuples of arity 1 by having a member with
    // the name of the first tuple field just default to returning the object.
    // TODO(bob): Cleaner solution.
    if (name.equals(Name.getTupleField(0))) {
      return receiver;
    }
    
    // If we get here, it wasn't found.
    return null;
  }
  
  private final InterpreterHost mHost;
  private Scope mGlobalScope;
  private final ClassObj mClass;
  private final ClassObj mArrayClass;
  private final ClassObj mBoolClass;
  private final ClassObj mFnClass;
  private final ClassObj mMultimethodClass;
  private final ClassObj mIntClass;
  private final ClassObj mMetaclass;
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
  private final Grammar mGrammar;
}
