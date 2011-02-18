package com.stuffwithstuff.magpie;

import java.io.*;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Profiler;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;

public class Magpie {

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
      if (niceRepl) {
        NiceRepl.run();
      } else {
        runRepl();
      }
    } else {
      runScript(path);
    }

    Profiler.display();
  }
  
  private static void runRepl() {
    System.out.println();
    System.out.println("      _/Oo>");
    System.out.println("     /__/     magpie v0.0.0");
    System.out.println("____//hh___________________");
    System.out.println("   //");
    System.out.println();
    System.out.println("Type 'quit()' and press <Enter> to exit.");

    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    // The REPL runs and imports relative to the current directory.
    interpreter.pushScriptPath(".");
    
    try {
      Script.loadBase(interpreter);
      
      while (true) {
        ReplCharacterReader reader = new ReplCharacterReader();
        Lexer lexer = new Lexer("REPL", reader);
        MagpieParser parser = interpreter.createParser(lexer);
        
        try {
          Expr expr = parser.parseExpression();
          
          String result = interpreter.evaluateToString(expr);
          if (result != null) {
            System.out.print(" = ");
            System.out.println(result);
          }
        } catch(ParseException ex) {
          System.out.println("!! Parse error: " + ex.getMessage());
        }
      }
    } catch (QuitException e) {
      // Do nothing.
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static void runScript(String path) {
    try {
      Script script = Script.fromPath(path);
      script.execute();
    } catch (QuitException e) {
      // Do nothing.
    } catch (IOException ex) {
      System.out.println("Could not load " + path);
    }
  }
}
