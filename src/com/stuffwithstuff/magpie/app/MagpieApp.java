package com.stuffwithstuff.magpie.app;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.ModuleInfo;
import com.stuffwithstuff.magpie.interpreter.Profiler;
import com.stuffwithstuff.magpie.interpreter.QuitException;

public class MagpieApp {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String path = null;
    
    // Process the arguments.
    boolean niceRepl = true;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-p")) {
        Profiler.setEnabled(true);
      } else if (args[i].equals("--dumbrepl")) {
        niceRepl = false;
      } else {
        if (i < args.length - 1) {
          System.out.println("Unrecognized argument: " + args[i]);
        } else {
          // The last argument is a script path.
          path = args[i];
        }
      }
    }
    
    // If no script is given, just spin up the REPL.
    if (path == null) {
      Repl repl = niceRepl ? new NiceRepl() : new Repl();
      repl.run();
    } else {
      runScript(path);
    }

    Profiler.display();
  }
  
  public static File getAppDirectory() {
    URL location = MagpieApp.class.getProtectionDomain().getCodeSource().getLocation();
    // Back up one directory to get out of "bin/".
    return new File(location.getFile()).getParentFile();
  }

  public static ModuleInfo loadModule(String name) {
    try {
      String modulePath = name.replace('.', '/');
      
      // $CWD/foo/bar.mag
      File file = new File(modulePath + ".mag");
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
      
      // $APPDIR/lib/foo/bar.mag
      File appDir = new File(getAppDirectory(), "lib");
      file = new File(appDir, modulePath + ".mag");
      if (file.exists()) {
        return new ModuleInfo(name,
            file.getPath(), readFile(file.getPath()));
      }
      
      // $APPDIR/lib/foo/bar/_init.mag
      file = new File(appDir, modulePath + "/_init.mag");
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
  
  private static void runScript(String path) {
    try {
      execute(path);
    } catch (QuitException e) {
      // Do nothing.
    } catch (IOException ex) {
      System.out.println("Could not load " + path);
    }
  }
}
