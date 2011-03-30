package com.stuffwithstuff.magpie.interpreter.builtin;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
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
}
