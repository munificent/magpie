package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.stuffwithstuff.magpie.parser.Position;
import com.stuffwithstuff.magpie.util.NotImplementedException;

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
  
  public void addMethod(Callable method) {
    mMethods.add(method);
  }
  
  public Obj invoke(Interpreter interpreter, Obj arg) {
    List<Callable> applicable = selectApplicable(interpreter, arg);
    if (applicable.size() == 0) {
      interpreter.throwError("NoMethodError", 
          "Could not find a method to match an argument of class " +
          arg.getClassObj().getName() + ".");
    }
    
    linearize(interpreter, applicable);
    
    // TODO(bob): When multimethods work with "this" (by having arg be a tuple
    // of (this, otherArg)) then pass it in here.
    return applicable.get(0).invoke(interpreter, interpreter.nothing(),
        new ArrayList<Obj>(), arg);
  }
  
  private List<Callable> selectApplicable(Interpreter interpreter, Obj arg) {
    // Ask the argument for its type.
    Obj argType = interpreter.getQualifiedMember(Position.none(),
        arg, null, Name.TYPE);

    List<Callable> applicable = new ArrayList<Callable>();
    for (Callable method : mMethods) {
      // See if the argument is assignable to the method's type.
      Obj type = getMethodType(interpreter, method);
      Obj matches = interpreter.invokeMethod(type, Name.ASSIGNS_FROM, argType);

      if (matches.asBool()) applicable.add(method);
    }
    
    return applicable;
  }
  
  private void linearize(Interpreter interpreter, List<Callable> methods) {
    if (methods.size() <= 1) return;
    Collections.sort(methods, new MethodLinearizer(interpreter));
  }
  
  private static Obj getMethodType(Interpreter interpreter, Callable method) {
    return method.getType(interpreter).getField(Name.PARAM_TYPE);
  }
  
  private final List<Callable> mMethods = new ArrayList<Callable>();
  
  private static class MethodLinearizer implements Comparator<Callable> {
    public MethodLinearizer(Interpreter interpreter) {
      mInterpreter = interpreter;
    }
    
    @Override
    public int compare(Callable method1, Callable method2) {
      Obj type1 = getMethodType(mInterpreter, method1);
      Obj type2 = getMethodType(mInterpreter, method2);
      
      return compare(type1, type2);
    }
    
    private int compare(Obj type1, Obj type2) {
      // TODO(bob): HACK work in progress...

      if (type1 == type2) return 0;
      
      // Classes are more specific than interfaces.
      if ((type1 instanceof ClassObj) &&
          type2.getClassObj().getName().equals("Interface")) {
        return -1;
      }
      
      if (type1.getClassObj().getName().equals("Interface") &&
          (type2 instanceof ClassObj)) {
        return 1;
      }
      
      if (type1.getClassObj().equals(mInterpreter.getTupleClass()) &&
          type2.getClassObj().equals(mInterpreter.getTupleClass())) {
        int count = type1.getField(Name.COUNT).asInt();
        int lastCompare = 0;
        for (int i = 0; i < count; i++) {
          int compare = compare(type1.getTupleField(i), type2.getTupleField(i));
          if (lastCompare < 0 && compare > 0) throw new NotImplementedException(
              "Don't handle inconsistent tuples yet.");
          if (lastCompare > 0 && compare < 0) throw new NotImplementedException(
              "Don't handle inconsistent tuples yet.");
          if (compare != 0) lastCompare = compare;
        }
        
        return lastCompare;
      }
      
      throw new NotImplementedException("Don't know now to linearize these types.");
      // A concrete class is more specific than a tuple or record.
      // A record is more specific than a tuple.
      // Everything is more specific than Any.
      // Tuples compare by fields.
      // Records compare by fields.
    }
    
    private final Interpreter mInterpreter;
  }
}
