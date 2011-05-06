package com.stuffwithstuff.magpie.interpreter;

import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

/**
 * The context a Callable has to operate within.
 */
public class Context {
  public Context(Module module) {
    mModule = module;
  }
  
  public Interpreter getInterpreter() {
    return mModule.getInterpreter();
  }
  
  public Module getModule() {
    return mModule;
  }
  
  public ErrorException error(String errorClassName, String message) {
    return getInterpreter().error(errorClassName, message);
  }
  
  public Obj evaluate(Expr expression, Scope scope) {
    return mModule.getInterpreter().evaluate(expression, mModule, scope);
  }
  
  public boolean objectsEqual(Obj a, Obj b) {
    return getInterpreter().objectsEqual(a, b);
  }
  
  public Obj nothing() {
    return getInterpreter().nothing();
  }
  
  public Obj toObj(boolean value) {
    return getInterpreter().createBool(value);
  }
  
  public Obj toObj(int value) {
    return getInterpreter().createInt(value);
  }
  
  public Obj toObj(String value) {
    return getInterpreter().createString(value);
  }

  public Obj toObj(Obj... fields) {
    return getInterpreter().createRecord(fields);
  }
  
  public Obj toObj(Map<String, Obj> fields) {
    return getInterpreter().createRecord(fields);
  }

  public Obj toList(List<Obj> elements) {
    return getInterpreter().createList(elements);
  }

  public Obj toFunction(FnExpr expr, Scope closure) {
    return getInterpreter().createFn(expr, closure);
  }

  private final Module mModule;  
}
