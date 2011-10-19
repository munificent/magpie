package com.stuffwithstuff.magpie.intrinsic;

import java.io.File;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class PathMethods {
  @Def("(this is String) baseName")
  @Doc("Gets the last component of the file path.")
  public static class BaseName implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      return context.toObj(new File(left.asString()).getName());
    }
  }
  
  @Def("(this is String) dirName")
  @Doc("Gets all of the components of the path except the last one.")
  public static class DirName implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String dirName = new File(left.asString()).getParent();
      if (dirName == null) return context.toObj("");
      return context.toObj(dirName);
    }
  }

  @Def("(is String) exists")
  @Doc("Returns true if the path exists.")
  public static class String_Exists implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String path = left.asString();
      return context.toObj(new File(path).exists());
    }
  }

  @Def("(is String) isDir")
  @Doc("Returns true if the path points to a directory.")
  public static class String_IsDir implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String path = left.asString();
      return context.toObj(new File(path).isDirectory());
    }
  }

  @Def("(is String) isFile")
  @Doc("Returns true if the path points to a file.")
  public static class String_IsFile implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String path = left.asString();
      return context.toObj(new File(path).isFile());
    }
  }
}
