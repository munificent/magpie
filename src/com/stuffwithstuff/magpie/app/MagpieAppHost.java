package com.stuffwithstuff.magpie.app;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.stuffwithstuff.magpie.MagpieHost;
import com.stuffwithstuff.magpie.SourceFile;
import com.stuffwithstuff.magpie.util.FileReader;

public class MagpieAppHost implements MagpieHost {
  @Override
  public SourceFile loadModule(String name) {
    try {
      String modulePath = name.replace('.', '/');

      // $CWD/foo/bar.mag
      File file = new File(modulePath + ".mag");
      if (file.exists()) {
        return new SourceFile(file.getPath(), readFile(file.getPath()));
      }
      
      // $CWD/foo/bar/_init.mag
      file = new File(modulePath + "/_init.mag");
      if (file.exists()) {
        return new SourceFile(file.getPath(), readFile(file.getPath()));
      }
      
      // $APPDIR/lib/foo/bar.mag
      File appDir = new File(getAppDirectory(), "lib");
      file = new File(appDir, modulePath + ".mag");
      if (file.exists()) {
        return new SourceFile(file.getPath(), readFile(file.getPath()));
      }
      
      // $APPDIR/lib/foo/bar/_init.mag
      file = new File(appDir, modulePath + "/_init.mag");
      if (file.exists()) {
        return new SourceFile(file.getPath(), readFile(file.getPath()));
      }

      throw new IOException("Couldn't find module " + name);
    } catch (IOException e) {
      e.printStackTrace();
      // TODO(bob): Handle error!
      return null;
    }
  }

  @Override
  public void showSyntaxError(String message) {
    System.out.println(message);
  }

  private static File getAppDirectory() {
    URL location = MagpieApp.class.getProtectionDomain().getCodeSource().getLocation();
    // Back up one directory to get out of "bin/".
    return new File(location.getFile()).getParentFile();
  }

  public static String readFile(String path) throws IOException {
    // If we're given a directory, look for _init.mag in it.
    File filePath = new File(path);
    if (filePath.isDirectory()) {
      path = new File(filePath, "_init.mag").getPath();
    }
    
    return FileReader.read(path);
  }
}