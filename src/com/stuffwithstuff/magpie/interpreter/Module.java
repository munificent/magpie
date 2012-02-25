package com.stuffwithstuff.magpie.interpreter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.stuffwithstuff.magpie.SourceReader;
import com.stuffwithstuff.magpie.SourceFile;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;
import com.stuffwithstuff.magpie.parser.StringReader;

public class Module implements Context {
  public Module(String name, SourceFile info, Interpreter interpreter) {
    mName = name;
    mInfo = info;
    mInterpreter = interpreter;
    mScope = new Scope(this);
  }
  
  public String getName() { return mName; }
  public Interpreter getInterpreter() { return mInterpreter; }
  public Scope getScope() { return mScope; }
  public Set<String> getExportedNames() { return mExportedNames; }
   
  public SourceReader readSource() {
    return new StringReader(mInfo.getPath(), mInfo.getSource());
  }
  
  public void export(String name) {
    mExportedNames.add(name);
  }
  
  // Context inferface:
  
  public Module getModule() {
    return this;
  }
  
  public ErrorException error(String errorClassName, String message) {
    return mInterpreter.error(errorClassName, message);
  }
  
  public Obj evaluate(Expr expression, Scope scope) {
    return mInterpreter.evaluate(expression, this, scope);
  }
  
  public boolean isBool(Obj object) {
    return mInterpreter.getBoolClass() == object.getClassObj();
  }
  
  public boolean isInt(Obj object) {
    return mInterpreter.getIntClass() == object.getClassObj();
  }
  
  public boolean isNothing(Obj object) {
    return mInterpreter.nothing() == object;
  }
  
  public boolean isString(Obj object) {
    return mInterpreter.getStringClass() == object.getClassObj();
  }
  
  public boolean objectsEqual(Obj a, Obj b) {
    return mInterpreter.objectsEqual(a, b);
  }
  
  public Obj instantiate(ClassObj classObj, Object primitiveValue) {
    Obj object = new Obj(classObj, primitiveValue);
    
    // Initialize its fields.
    for (Entry<String, FieldObj> field : classObj.getFieldDefinitions().entrySet()) {
      Callable initializer = field.getValue().getInitializer();
      if (initializer != null) {
        Obj value = initializer.invoke(this, nothing());
        object.setField(field.getKey(), value);
      }
    }
    
    return object;
  }
  
  public Obj nothing() {
    return mInterpreter.nothing();
  }
  
  public Obj toObj(boolean value) {
    return mInterpreter.getBool(value);
  }
  
  public Obj toObj(int value) {
    return instantiate(mInterpreter.getIntClass(), value);
  }
  
  public Obj toObj(String value) {
    return instantiate(mInterpreter.getStringClass(), value);
  }

  public Obj toObj(Obj... fields) {
    Obj record = instantiate(mInterpreter.getRecordClass(), null);
    
    int index = 0;
    for (Obj field : fields) {
      record.setField(Name.getTupleField(index++), field);
    }
    
    return record;
  }
  
  public Obj toObj(List<String> keys, Map<String, Obj> fields) {
    Obj record = instantiate(mInterpreter.getRecordClass(), keys);
    
    for (Entry<String, Obj> field : fields.entrySet()) {
      record.setField(field.getKey(), field.getValue());
    }
    
    return record;
  }

  public Obj toArray(List<Obj> elements) {
    return instantiate(mInterpreter.getArrayClass(), elements);
  }

  public Obj toList(List<Obj> elements) {
    return instantiate(mInterpreter.getListClass(), elements);
  }

  public Obj toFunction(FnExpr expr, Scope closure) {
    return new FnObj(mInterpreter.getFnClass(), new Function(expr, closure));
  }
  
  // End Context
  
  @Override
  public String toString() {
    return mInfo.toString();
  }
  
  private final String mName;
  private final SourceFile mInfo;
  private final Interpreter mInterpreter;
  private final Scope mScope;
  private final Set<String> mExportedNames = new HashSet<String>();
}
