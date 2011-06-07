package com.stuffwithstuff.magpie.app;

import java.io.IOException;

import com.stuffwithstuff.magpie.Magpie;
import com.stuffwithstuff.magpie.Method;
import com.stuffwithstuff.magpie.SourceFile;
import com.stuffwithstuff.magpie.interpreter.Profiler;

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
      ConsoleRepl repl = niceRepl ? new ColorRepl() : new ConsoleRepl();
      repl.run();
    } else {
      runScript(path);
    }

    Profiler.display();
  }
  
  public static void execute(String path) throws IOException {
    String script = MagpieAppHost.readFile(path);
    Magpie magpie = new Magpie(new MagpieAppHost());
    
    magpie.defineMethod("printString(s is String)",
        "Prints the given string to stdout.", new Method() {
      public Object call(Object left, Object right) {
        System.out.print(right);
        return null;
      }
    });
    
    String result = magpie.run(new SourceFile(path, script));
    if (result != null) {
      System.out.println(result);
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
