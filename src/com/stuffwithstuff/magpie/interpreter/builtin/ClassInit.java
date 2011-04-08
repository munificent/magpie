package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.Field;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Built-in callable that initializes an instance of a class from the given
 * record of fields and parent class initializers.
 */
public class ClassInit implements Callable {
  public ClassInit(ClassObj classObj, Scope closure) {
    mClass = classObj;
    mClosure = closure;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    // We don't care about the receiver.
    arg = arg.getTupleField(1);
    
    Obj obj = interpreter.getConstructingObject();

    // Initialize the parent classes from the record.
    for (ClassObj parent : mClass.getParents()) {
      Obj value = arg.getField(parent.getName());
      if (value != null) {
        interpreter.initializeNewObject(parent, value);
      }
    }
    
    // Initialize the fields from the record.
    for (Entry<String, Field> field : mClass.getFieldDefinitions().entrySet()) {
      // Only care about fields that don't have initializers.
      if (field.getValue().getInitializer() == null) {
        // Assign it from the record.
        Obj value = arg.getField(field.getKey());
        if (value != null) {
          obj.setField(field.getKey(), arg.getField(field.getKey()));
        }
      }
    }
    
    return obj;
  }
  
  @Override
  public Pattern getPattern() {
    // The receiver should be the class object itself.
    Pattern receiver = Pattern.value(Expr.variable(mClass.getName()));
    
    // The argument should be a record with fields for each declared field
    // in the class.
    List<Pair<String, Pattern>> fields = new ArrayList<Pair<String, Pattern>>();
    for (Entry<String, Field> field : mClass.getFieldDefinitions().entrySet()) {
      // Only care about fields that don't have initializers.
      if (field.getValue().getInitializer() == null) {
        fields.add(new Pair<String, Pattern>(field.getKey(),
            field.getValue().getPattern()));
      }
    }

    Pattern argument = Pattern.record(fields);
    
    return Pattern.tuple(receiver, argument);
  }

  @Override
  public Scope getClosure() {
    return mClosure;
  }

  @Override
  public String toString() {
    return mClass.getName() + " init(" + getPattern() + ")";
  }
  
  private final ClassObj mClass;
  private final Scope mClosure;
}
