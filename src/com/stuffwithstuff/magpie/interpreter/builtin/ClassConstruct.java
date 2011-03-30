package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Field;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;

/**
 * Built-in callable that takes a record and creates an instance of a class
 * whose fields are initialized with the record's fields.
 */
public class ClassConstruct implements Callable {
  @Override
  public Callable bindTo(ClassObj classObj) {
    return this;
  }

  @Override
  public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
    ClassObj classObj = thisObj.asClass();
    
    Obj obj = interpreter.instantiate(classObj, null);

    // Initialize or assign the fields.
    for (Entry<String, Field> field : classObj.getFieldDefinitions().entrySet()) {
      if (!field.getValue().hasInitializer()) {
        // Assign it from the record.
        Obj value = arg.getField(field.getKey());
        if (value != null) {
          obj.setField(field.getKey(), arg.getField(field.getKey()));
        }
      }
    }
    
    return obj;
  }
}
