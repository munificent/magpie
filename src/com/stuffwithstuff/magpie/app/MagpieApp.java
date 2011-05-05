package com.stuffwithstuff.magpie.app;

import java.io.*;

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
  
  private static void runScript(String path) {
    try {
      Script.execute(path);
    } catch (QuitException e) {
      // Do nothing.
    } catch (IOException ex) {
      System.out.println("Could not load " + path);
    }
  }
}
