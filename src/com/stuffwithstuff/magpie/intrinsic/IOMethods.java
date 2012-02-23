package com.stuffwithstuff.magpie.intrinsic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Doc;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.util.FileReader;
import com.stuffwithstuff.magpie.util.FileWriter;

public class IOMethods {
  // TODO(bob): There is a big hack here. We use the same "File" Magpie class
  // to encapsulate both a FileReader and a FileWriter. But the read and
  // write methods assume one or the other and will blow up if you use the
  // wrong one.
  
  // TODO(bob): Hackish.
  @Def("_setClasses(== File)")
  public static class SetClasses implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      sFileClass = right.asClass();
      return context.nothing();
    }
  }

  @Def("(is File) close()")
  @Doc("Closes the file.")
  public static class Close implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      if (left.getValue() instanceof FileReader) {
        FileReader reader = (FileReader)left.getValue();
        reader.close();
      } else {
        FileWriter writer = (FileWriter)left.getValue();
        writer.close();
      }
      return context.nothing();
    }
  }

  @Def("open(path is String)")
  @Doc("Opens the file at the given path.")
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

  @Def("create(path is String)")
  @Doc("Creates a new file at the given path.")
  public static class Create implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String path = right.asString();
      
      FileWriter writer;
      try {
        writer = new FileWriter(path);
        return context.instantiate(sFileClass, writer);
      } catch (IOException e) {
        throw context.error("IOError", "Could not open file.");
      }
    }
  }
  
  @Def("(is File) isOpen")
  @Doc("Returns true if the file is open.")
  public static class IsOpen implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileReader reader = (FileReader)left.getValue();
      return context.toObj(reader.isOpen());
    }
  }
  
  @Def("(is File) read()")
  @Doc("Reads the contents of the file as a string.")
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
  @Doc("Reads a single line of text from the file. Returns nothing if at\n" +
       "the end of the file.")
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

  @Def("readLine()")
  @Doc("Reads a line from standard input and returns it.")
  public static class Nothing_ReadLine implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String string;
      try {
        string = in.readLine();
        return context.toObj(string);
      } catch (IOException e) {
        // TODO(bob): Handle error.
        e.printStackTrace();
        throw context.error("IOError", "Could not read.");
      }
    }
  }

  @Def("(is File) writeByte(is Int)")
  @Doc("Writes the given byte (value from 0 to 255 inclusive) to this File.")
  public static class WriteByte implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileWriter writer = (FileWriter)left.getValue();
      try {
        writer.writeByte(right.asInt());
        return right;
      } catch (IOException e) {
        throw context.error("IOError", "Could not write.");
      }
    }
  }

  @Def("(is File) writeInt32(is Int)")
  @Doc("Writes the given int to this File.")
  public static class WriteInt implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileWriter writer = (FileWriter)left.getValue();
      try {
        writer.writeInt32(right.asInt());
        return right;
      } catch (IOException e) {
        throw context.error("IOError", "Could not write.");
      }
    }
  }

  // TODO(bob): Need to support non-ints!
  @Def("(is File) writeDouble(is Int)")
  @Doc("Writes the given number to this File.")
  public static class WriteDouble implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileWriter writer = (FileWriter)left.getValue();
      try {
        writer.writeDouble(right.asInt());
        return right;
      } catch (IOException e) {
        throw context.error("IOError", "Could not write.");
      }
    }
  }

  @Def("(is File) writeUInt16(is Int)")
  @Doc("Writes the given int to this File.")
  public static class WriteUInt16 implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileWriter writer = (FileWriter)left.getValue();
      try {
        writer.writeUInt16(right.asInt());
        return right;
      } catch (IOException e) {
        throw context.error("IOError", "Could not write.");
      }
    }
  }
  
  @Def("(is File) writeUInt32(is Int)")
  @Doc("Writes the given int to this File.")
  public static class WriteUInt32 implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileWriter writer = (FileWriter)left.getValue();
      try {
        writer.writeUInt32(right.asInt());
        return right;
      } catch (IOException e) {
        throw context.error("IOError", "Could not write.");
      }
    }
  }
  
  @Def("(is Directory) _contents")
  @Doc("Gets the contents of the directory.")
  public static class Directory_Iterate implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      String path = left.getField("path").asString();
      File[] files = new File(path).listFiles();
      
      List<Obj> paths = new ArrayList<Obj>();
      for (File file : files) {
        paths.add(context.toObj(file.getPath()));
      }
      
      return context.toList(paths);
    }
  }
  
  private static ClassObj sFileClass;
}
