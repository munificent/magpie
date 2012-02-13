package com.stuffwithstuff.magpie.util;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {
  public FileWriter(String path) throws IOException {
    mStream = new DataOutputStream(new FileOutputStream(path));
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
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public void writeByte(int value) throws IOException {
    mStream.writeByte(value);
  }
  
  public void writeInt(int value) throws IOException {
    mStream.writeInt(value);
  }
  
  private DataOutputStream mStream;
}