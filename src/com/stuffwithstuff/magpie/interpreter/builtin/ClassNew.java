package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.Collections;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FunctionType;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.Expect;

/**
 * Built-in callable that constructs a new instance of some class.
 */
// TODO(bob): This is pretty much temp.
public class ClassNew implements Callable {
  public ClassNew(String className) {
    Expect.notEmpty(className);
    
    mClassName = className;
  }
  
  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    ClassObj classObj = (ClassObj)thisObj;
    
    Obj obj = interpreter.instantiate(classObj, null);
        
    // TODO(bob): Needs to call parent constructors too!
    
    // Find and call the constructor (if any).
    Callable constructor = classObj.getConstructor();
    if (constructor != null) {
      constructor.invoke(interpreter, obj, arg);
    }
    
    return obj;
  }

  public FunctionType getType() {
    return new FunctionType(Collections.singletonList("arg"),
        Expr.name("Dynamic"), Expr.name(mClassName));
  }
  
  private final String mClassName;
}
