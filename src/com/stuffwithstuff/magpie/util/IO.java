package com.stuffwithstuff.magpie.util;

import java.io.BufferedReader;
import java.io.IOException;

public class IO {
  public static String readAll(BufferedReader reader) throws IOException {
    StringBuilder builder = new StringBuilder();
    char[] buffer = new char[8192];
    int read;

    while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
      builder.append(buffer, 0, read);
    }

    return builder.toString();
  }
}