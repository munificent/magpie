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
  
  public void writeInt32(int value) throws IOException {
    mStream.writeInt(value);
  }
  
  public void writeUInt16(int value) throws IOException {
    // Little endian.
    mStream.writeByte(value & 0x00ff);
    mStream.writeByte((value & 0xff00) >> 8);
  }
  
  public void writeUInt32(int value) throws IOException {
    // Little endian.
    mStream.writeByte(value & 0x000000ff);
    mStream.writeByte((value & 0x0000ff00) >>  8);
    mStream.writeByte((value & 0x00ff0000) >> 16);
    mStream.writeByte((value & 0xff000000) >> 24);
  }
  
  public void writeDouble(double value) throws IOException {
    mStream.writeDouble(value);
  }

  private DataOutputStream mStream;
}