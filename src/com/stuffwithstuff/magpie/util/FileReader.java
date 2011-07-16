package com.stuffwithstuff.magpie.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class FileReader {
  public static String read(String path) throws IOException {
    FileReader reader = null;
    try {
      reader = new FileReader(path);
      return reader.readAll();
    } finally {
      if (reader != null) reader.close();
    }
  }
  
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
  
  public String readAll() throws IOException {
    return IO.readAll(mReader);
  }
  
  private FileInputStream mStream;
  private BufferedReader mReader;
}