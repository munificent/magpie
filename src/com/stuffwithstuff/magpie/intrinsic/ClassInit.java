package com.stuffwithstuff.magpie.intrinsic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.FieldObj;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.Scope;

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
  public Obj invoke(Context context, Obj arg) {
    // We don't care about the receiver.
    arg = arg.getField(1);
    
    Obj obj = context.getInterpreter().getConstructingObject();

    // Initialize the parent classes from the record.
    for (ClassObj parent : mClass.getParents()) {
      Obj value = arg.getField(parent.getName());
      if (value != null) {
        context.getInterpreter().initializeNewObject(context, parent, value);
      }
    }
    
    // Initialize the fields from the record.
    for (Entry<String, FieldObj> field : mClass.getFieldDefinitions().entrySet()) {
      // Assign it from the record.
      Obj value = arg.getField(field.getKey());
      if (value != null) {
        obj.setField(field.getKey(), arg.getField(field.getKey()));
      }
    }
    
    // Note that we've successfully reached the canonical initializer.
    context.getInterpreter().finishInitialization();
    
    return obj;
  }
  
  @Override
  public Pattern getPattern() {
    // The receiver should be the class object itself.
    Pattern receiver = Pattern.value(Expr.name(mClass.getName()));
    
    // The argument should be a record with fields for each declared field
    // in the class.
    Map<String, Pattern> fields = new HashMap<String, Pattern>();
    for (Entry<String, FieldObj> field : mClass.getFieldDefinitions().entrySet()) {
      // Only care about fields that don't have initializers.
      if (field.getValue().getInitializer() == null) {
        fields.put(field.getKey(), field.getValue().getPattern());
      }
    }

    Pattern argument = Pattern.record(fields);
    
    return Pattern.record(receiver, argument);
  }

  @Override
  public Scope getClosure() {
    return mClosure;
  }
  
  @Override
  public String getDoc() {
    return "Canonical initializer for class " + mClass.getName() + ".";
  }

  @Override
  public String toString() {
    return mClass.getName() + " init(" + getPattern() + ")";
  }
  
  private final ClassObj mClass;
  private final Scope mClosure;
}
