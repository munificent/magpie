package com.stuffwithstuff.magpie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Module;
import com.stuffwithstuff.magpie.interpreter.ModuleInfo;

public class Script {
  public static ModuleInfo loadModule(Module loadingModule, String name) {
    try {
      // Given name "foo.bar" and path "here/", we'll try:
      // here/foo/bar.mag
      // here/foo/bar/_init.mag
      // $CWD/foo/bar.mag
      // $CWD/foo/bar/_init.mag
      
      File scriptPath = new File(loadingModule.getPath());
      String scriptDir;
      if (scriptPath.isDirectory()) {
        scriptDir = scriptPath.getPath();
      } else {
        scriptDir = scriptPath.getParent();
      }
      String modulePath = name.replace('.', '/');
      
      // here/foo/bar.mag
      File file = new File(scriptDir, modulePath + ".mag");
      if (file.exists()) {
        return new ModuleInfo(getModuleName(loadingModule.getName(), name),
            file.getPath(), readFile(file.getPath()));
      }
      
      // here/foo/bar/_init.mag
      file = new File(scriptDir, modulePath + "/_init.mag");
      if (file.exists()) {
        return new ModuleInfo(getModuleName(loadingModule.getName(), name),
            file.getPath(), readFile(file.getPath()));
      }
      
      // $CWD/foo/bar.mag
      file = new File(modulePath + ".mag");
      if (file.exists()) {
        return new ModuleInfo(name,
            file.getPath(), readFile(file.getPath()));
      }
      
      // $CWD/foo/bar/_init.mag
      file = new File(modulePath + "/_init.mag");
      if (file.exists()) {
        return new ModuleInfo(name,
            file.getPath(), readFile(file.getPath()));
      }
      
      throw new IOException("Couldn't find module " + name);
    } catch (IOException e) {
      e.printStackTrace();
      // TODO(bob): Handle error!
      return null;
    }
  }
  
  private static String getModuleName(String parent, String name) {
    if (parent.length() == 0) return name;
    return parent + "." + name;
  }
  
  public static void execute(String path) throws IOException {
    String script = readFile(path);
    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    try {
      interpreter.interpret(new ModuleInfo(path, path, script));
    } catch(ErrorException ex) {
      System.out.println(String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue()));
    }
  }

  private static String readFile(String path) throws IOException {
    // If we're given a directory, look for _init.mag in it.
    File filePath = new File(path);
    if (filePath.isDirectory()) {
      path = new File(filePath, "_init.mag").getPath();
    }
    
    FileInputStream stream = new FileInputStream(path);

    try {
      InputStreamReader input = new InputStreamReader(
          stream, Charset.forName("UTF-8"));
      Reader reader = new BufferedReader(input);

      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[8192];
      int read;

      while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
        builder.append(buffer, 0, read);
      }

      return builder.toString();
    } finally {
      stream.close();
    }
  }
}
