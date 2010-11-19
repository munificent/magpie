package com.stuffwithstuff.magpie.interpreter.builtin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import com.stuffwithstuff.magpie.Script;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.ParseException;

public class ObjectBuiltIns {

  @Signature("==(other -> Bool)")
  public static class EqEq implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(thisObj == arg);
    }
  }

  @Getter("fields(Array newType(String, Dynamic))")
  public static class Fields implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      List<Entry<String, Obj>> fields = new ArrayList<Entry<String, Obj>>(
          thisObj.getFields().entries());
      
      // Sort by name.
      Collections.sort(fields, new Comparator<Entry<String, Obj>>() {
        public int compare(Entry<String, Obj> left, Entry<String, Obj> right) {
          return left.getKey().compareTo(right.getKey());
        }
      });
      
      // Convert to a list of tuples.
      List<Obj> results = new ArrayList<Obj>();
      for (Entry<String, Obj> field : fields) {
        results.add(interpreter.createTuple(
            interpreter.createString(field.getKey()),
            field.getValue()));
      }
      
      return interpreter.createArray(results);
    }
  }
  
  @Signature("getField(name String)")
  public static class GetField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      Obj field = thisObj.getField(name);
      if (field == null) return interpreter.nothing();
      
      return field;
    }
  }

  @Signature("hasField?(name String)")
  public static class HasField implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.asString();
      
      Obj field = thisObj.getField(name);
      return interpreter.createBool(field != null);
    }
  }

  @Signature("import(path String ->)")
  public static class Import_ implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String currentDir = new File(interpreter.getCurrentScript()).getParent();
      String relativePath = arg.asString();
      File scriptFile = new File(currentDir, relativePath);
      
      try {
        Script script = Script.fromPath(scriptFile.getPath());
        script.execute(interpreter);
      } catch (ParseException e) {
        interpreter.runtimeError(
            "Could not parse script \"%s\".\nError: %s",
            relativePath, e.getMessage());
      } catch (IOException e) {
        interpreter.runtimeError("Could not load script \"%s\"\n%s.",
            relativePath, e);
      }
      
      return interpreter.nothing();
    }
  }
  
  @Signature("printRaw(text String ->)")
  public static class PrintRaw implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(arg.asString());
      return interpreter.nothing();
    }
  }
  
  @Getter("type(-> Type)")
  public static class Type implements BuiltInCallable {
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getClassObj();
    }
  }
}
