package com.stuffwithstuff.magpie.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.util.Expect;

public class Obj {  
  public Obj(ClassObj classObj, Object value) {
    if (classObj == null) {
      // If we are a class, we're our own class.
      mClass = (this instanceof ClassObj) ? (ClassObj)this : null;
    } else {
      mClass = classObj;
    }
    
    mValue = value;
  }
  
  public Obj(ClassObj classObj) {
    this(classObj, null);
  }
  
  public ClassObj getClassObj() { return mClass; }
  
  public void bindClass(ClassObj classObj) {
    Expect.notNull(classObj);
    
    mClass = classObj;
  }
  
  /**
   * Gets the value of a given field.
   * @param name   The name of the field.
   * @return The value or null if there is no field with that name.
   */
  public Obj getField(String name) {
    return mFields.get(name);
  }
  
  public Obj getField(int index) {
    return getField(Name.getTupleField(index));
  }

  public Map<String, Obj> getFields() {
    return mFields;
  }
  
  /**
   * Sets the given field to the given value.
   * @param name   The name of the field.
   * @param member The fields's value.
   */
  public void setField(String name, Obj field) {
    mFields.put(name, field);
  }
  
  public Object getValue() {
    return mValue;
  }
  
  public void setValue(Object value) {
    mValue = value;
  }

  public boolean isClass() {
    return this instanceof ClassObj;
  }

  public boolean asBool() {
    if (mValue instanceof Boolean) {
      return ((Boolean)mValue).booleanValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a boolean.", this));
  }

  public ClassObj asClass() {
    if (this instanceof ClassObj) {
      return (ClassObj)this;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a class.", this));
  }
  
  public FnObj asFn() {
    if (this instanceof FnObj) {
      return (FnObj)this;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a function.", this));
  }

  @SuppressWarnings("unchecked")
  public List<Obj> asList() {
    if (mValue instanceof List<?>) {
      return (List<Obj>)mValue;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a List.", this));
  }
  
  public int asInt() {
    if (mValue instanceof Integer) {
      return ((Integer)mValue).intValue();
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not an int.", this));
  }
  
  public String asString() {
    if (mValue instanceof String) {
      return (String)mValue;
    }
    
    throw new InterpreterException(String.format(
        "The object \"%s\" is not a string.", this));
  }
  
  @Override
  public String toString() {
    if (mValue instanceof String) {
      return "\"" + mValue + "\"";
    } else if (mValue != null) {
      return mValue.toString();
    } else if (mClass.getName().equals("Record")) {
      StringBuilder builder = new StringBuilder();
      
      // Show fields in sorted order.
      List<String> fields = new ArrayList<String>(mFields.keySet());
      Collections.sort(fields);
      
      for (int i = 0; i < fields.size(); i++) {
        if (i > 0) builder.append(", ");
        String field = fields.get(i);
        builder.append(field).append(": ").append(mFields.get(field));
      }
      
      return builder.toString();
      
    } else if (mClass.getName().equals("Nothing")) {
      return "nothing";
    }

    return "Instance of " + mClass.getName();
  }
  
  private ClassObj mClass;
  private Object mValue;
  private final Map<String, Obj> mFields = new HashMap<String, Obj>();
}
