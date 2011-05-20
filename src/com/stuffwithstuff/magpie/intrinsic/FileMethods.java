package com.stuffwithstuff.magpie.intrinsic;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.interpreter.ClassObj;
import com.stuffwithstuff.magpie.interpreter.Context;
import com.stuffwithstuff.magpie.interpreter.Obj;

public class FileMethods {
  @Def("(is File) close()")
  public static class Close implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      FileReader reader = (FileReader)left.getValue();
      reader.close();
      return context.nothing();
    }
  }

  @Def("(== File) open(path is String)")
  public static class Open implements Intrinsic {
    public Obj invoke(Context context, Obj left, Obj right) {
      ClassObj fileClass = left.asClass();
      String path = right.asString();
      
      FileReader reader;
      try {
        reader = new FileReader(path);
        return context.instantiate(fileClass, reader);
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

  private static class FileReader {
    public FileReader(String path) throws IOException {
      mStream = new FileInputStream(path);
      InputStreamReader input = new InputStreamReader(mStream,
          Charset.forName("UTF-8"));
      mReader = new BufferedReader(input);
    }
    
    public boolean isOpen() {
      return mStream != null;
    }
    
    public void close() {
      // Do nothing if already closed.
      if (mStream == null) return;
      
      try {
        mStream.close();
        mStream = null;
        mReader = null;
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    public String readLine() throws IOException {
      return mReader.readLine();
    }
    
    private FileInputStream mStream;
    private BufferedReader mReader;
  }
  
}
