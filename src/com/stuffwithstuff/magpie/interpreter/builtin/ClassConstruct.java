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
import com.stuffwithstuff.magpie.util.Pair;

/**
 * Built-in callable that takes a record and creates an instance of a class
 * whose fields are initialized with the record's fields.
 */
public class ClassConstruct implements Callable {
  public ClassConstruct(ClassObj classObj) {
    mClass = classObj;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj arg) {
    // TODO(bob): Hack temp. This is called from multimethod, so arg is a tuple
    // of both receiver and right-hand arg. We just want right-hand.
    arg = arg.getTupleField(1);
    
    Obj obj = interpreter.instantiate(mClass, null);

    // Assign the fields.
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
    Pattern receiver = Pattern.value(Expr.name(mClass.getName()));
    
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
  
  private final ClassObj mClass;
}
