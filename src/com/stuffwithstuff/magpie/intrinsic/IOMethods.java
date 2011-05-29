package com.stuffwithstuff.magpie.intrinsic;

import java.io.IOException;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.FileReader;

public class IOMethods {
  // TODO(bob): Hackish.
  @Def("_setClasses(== File)")
  public static class SetClasses implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      sFileClass = right.asClass();
      return context.nothing();
    }
  }

  @Def("(is File) close()")
  public static class Close implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileReader reader = (FileReader)left.getValue();
      reader.close();
      return context.nothing();
    }
  }

  @Def("open(path is String)")
  public static class Open implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String path = right.asString();
      
      FileReader reader;
      try {
        reader = new FileReader(path);
        return context.instantiate(sFileClass, reader);
      } catch (IOException e) {
        throw context.error("IOError", "Could not open file.");
      }
    }
  }
  
  @Def("(is File) open?")
  public static class OpenP implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileReader reader = (FileReader)left.getValue();
      return context.toObj(reader.isOpen());
    }
  }
  
  @Def("(is File) read()")
  public static class Read implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileReader reader = (FileReader)left.getValue();
      try {
        String contents = reader.readAll();
        if (contents == null) return context.nothing();
        return context.toObj(contents);
      } catch (IOException e) {
        throw context.error("IOError", "Could not read.");
      }
    }
  }
  
  @Def("(is File) readLine()")
  public static class ReadLine implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileReader reader = (FileReader)left.getValue();
      try {
        String line = reader.readLine();
        if (line == null) return context.nothing();
        return context.toObj(line);
      } catch (IOException e) {
        throw context.error("IOError", "Could not read.");
      }
    }
  }
  
  private static ClassObj sFileClass;
}
