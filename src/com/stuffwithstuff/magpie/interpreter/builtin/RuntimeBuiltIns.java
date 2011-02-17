package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.CheckError;
import com.stuffwithstuff.magpie.interpreter.Checker;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.EvalContext;
import com.stuffwithstuff.magpie.interpreter.FnObj;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.NullInterpreterHost;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class RuntimeBuiltIns {
  // TODO(bob): All of these are pretty hacked together. Need to rationalize
  // the scope for these and clean them up.
  
  @Shared
  @Signature("now(-> Int)")
  public static class Now implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      // TODO(bob): Total hack to fit in an int.
      int time = (int) (System.currentTimeMillis() - 1289000000000L);
      return interpreter.createInt(time);
    }
  }
  
  @Shared
  @Signature("throw(obj -> Never)")
  public static class Throw_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      throw new ErrorException(arg);
    }
  }
  
  @Shared
  @Signature("checkAll(-> List(String))")
  public static class CheckAll implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      Checker checker = new Checker(interpreter);
      
      checker.checkAll();
      return translateErrors(interpreter, checker.getErrors());
    }
  }
  
  @Shared
  @Signature("checkClass(classObj Class-> List(String))")
  public static class CheckClass implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      Checker checker = new Checker(interpreter);
      
      checker.checkClass(arg.asClass());
      
      return translateErrors(interpreter, checker.getErrors());
    }
  }

  @Shared
  @Signature("checkFunction(function -> List(String))")
  public static class CheckFunction implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      Checker checker = new Checker(interpreter);
      
      FnObj function = arg.asFn();
      EvalContext staticContext = interpreter.createTopLevelContext();
      checker.checkFunction(function.getFunction(),
          interpreter.getNothingClass(), staticContext);
      
      return translateErrors(interpreter, checker.getErrors());
    }
  }

  @Shared
  @Signature("checkExpression(function -> List(String))")
  public static class CheckExpression implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj function = arg.asFn();
      Expr expr = function.getFunction().getFunction().getBody();
      
      Interpreter inner = new Interpreter(new NullInterpreterHost());
      try {
        Script.loadBase(inner);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      inner.interpret(expr);
      
      // Do the static analysis and see if we got the errors we expect.
      Checker checker = new Checker(inner);
      checker.checkAll();
      
      return translateErrors(interpreter, checker.getErrors());
    }
  }
  
  @Shared
  @Signature("debugDump(object ->)")
  public static class DebugDump implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      
      StringBuilder builder = new StringBuilder();
      dumpObject(builder, arg, "", "");
      System.out.print(builder);
      
      return interpreter.nothing();
    }
  }
  
  private static void dumpObject(StringBuilder builder, Obj object,
      String name, String indent) {
    builder.append(indent);
    
    if (name.length() > 0) {
      builder.append(name).append(": ");
    }
    
    if (object.getValue() != null) {
      builder.append(object.getValue()).append(" ");
    }
    
    builder.append("(").append(object.getClassObj().getName()).append(")\n");
    
    // Don't recurse too deep in case we have a cyclic structure.
    if (indent.length() > 6) return;
    
    for (Entry<String, Obj> field : object.getFields().entries()) {
      dumpObject(builder, field.getValue(), field.getKey(), indent + "  ");
    }
  }

  private static Obj translateErrors(Interpreter interpreter, List<CheckError> errors) {
    List<Obj> errorObjs = new ArrayList<Obj>();
    for (CheckError error : errors) {
      // TODO(bob): Should eventually return more than just the error message.
      errorObjs.add(interpreter.createString(error.toString()));
    }

    return interpreter.createArray(errorObjs);
  }
}
