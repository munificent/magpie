package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stuffwithstuff.magpie.util.Expect;

// TODO(bob): This class is completely hacked together and comically slow. It's
// barely even a proof of concept.
/**
 * Object type for a multimethod object.
 */
public class MultimethodObj extends Obj {
  /**
   * Creates a new MethodObj.
   * 
   * @param classObj     The class of the method object itself: Method.
   */
  public MultimethodObj(ClassObj classObj) {
    super(classObj);
  }
  
  public List<Callable> getMethods() { return mMethods; }
  
  public void addMethod(Callable method) {
    mMethods.add(method);
  }
  
  public Obj invoke(Interpreter interpreter, Obj receiver, boolean isExplicit, Obj arg) {
    // If we're given a right-hand argument, combine it with the receiver.
    // If not, this is a getter-style multimethod.
    Obj argTuple;
    if (arg != null) {
      argTuple = interpreter.createTuple(receiver, arg);
    } else {
      // The receiver is the entire argument.
      argTuple = receiver;
    }
    Callable method = select(interpreter, argTuple);
    
    // If we couldn't find a method on the implicit receiver, see if there's a
    // receiverless method available.
    if ((method == null) && !isExplicit && (arg != null)) {
      argTuple = interpreter.createTuple(interpreter.nothing(), arg);
      method = select(interpreter, argTuple);
    }
    
    if (method == null) {
      interpreter.error("NoMethodError", 
          "Could not find a method to match argument " + argTuple + ".");
    }

    return method.invoke(interpreter, argTuple);
  }
  
  private Callable select(Interpreter interpreter, Obj arg) {
    Expect.notNull(arg);
    
    List<Callable> applicable = new ArrayList<Callable>();
    for (Callable method : mMethods) {
      // TODO(bob): Should this be a top level context?
      if (PatternTester.test(interpreter, method.getPattern(), arg,
            interpreter.createTopLevelContext())) {
        applicable.add(method);
      }
    }
    
    if (applicable.size() == 0) return null;

    linearize(interpreter, applicable);
    
    return applicable.get(0);
  }

  private void linearize(Interpreter interpreter, List<Callable> methods) {
    if (methods.size() <= 1) return;
    Collections.sort(methods, new MethodLinearizer(interpreter));
  }
  
  private final List<Callable> mMethods = new ArrayList<Callable>();
}
