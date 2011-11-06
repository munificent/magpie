package com.stuffwithstuff.magpie.interpreter;

import java.util.List;
import java.util.Map;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.FnExpr;

/**
 * The context a Callable has to operate within.
 */
public interface Context {
  public Interpreter getInterpreter();
  public Module getModule();
  public ErrorException error(String errorClassName, String message);
  public Obj evaluate(Expr expression, Scope scope);
  public boolean isBool(Obj object);
  public boolean isInt(Obj object);
  public boolean isNothing(Obj object);
  public boolean isString(Obj object);
  public boolean objectsEqual(Obj a, Obj b);
  public Obj instantiate(ClassObj classObj, Object primitiveValue);
  public Obj nothing();
  public Obj toObj(boolean value);
  public Obj toObj(int value);
  public Obj toObj(String value);
  public Obj toObj(Obj... fields);
  public Obj toObj(List<String> keys, Map<String, Obj> fields);
  public Obj toArray(List<Obj> elements);
  public Obj toList(List<Obj> elements);
  public Obj toFunction(FnExpr expr, Scope closure);
}
