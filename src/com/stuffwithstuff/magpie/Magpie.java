package com.stuffwithstuff.magpie;

import java.io.*;
import java.util.*;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;

public class Magpie {

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      // With no command line args, runs the REPL.
      runRepl();
    } else if (args.length == 1) {
      if (args[0].equals("-t")) {
        System.out.println("Running test suite...");
        runTestScripts();
      } else {
        // One command line arg: load the script at that path and run it.
        runScript(args[0]);
      }
    }
    
    //Profiler.display();
  }
  
  private static void runRepl() {
    System.out.println();
    System.out.println("      _/Oo>");
    System.out.println("     /__/     magpie v0.0.0");
    System.out.println("____//hh___________________");
    System.out.println("   //");
    System.out.println();
    
    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    // The REPL runs and imports relative to the current directory.
    interpreter.pushScriptPath(".");
    
    try {
      Script.loadBase(interpreter);
      
      while (true) {
        ReplCharacterReader reader = new ReplCharacterReader();
        Lexer lexer = new Lexer("REPL", reader);
        MagpieParser parser = new MagpieParser(lexer);
        
        Expr expr = parser.parseExpression();
        
        String result = interpreter.evaluateToString(expr);
        if (result != null) {
          System.out.print(":: ");
          System.out.println(result);
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static void runScript(String path) {
    try {
      Script script = Script.fromPath(path);
      script.execute();
    } catch (IOException ex) {
      System.out.println("Could not load " + path);
    }
  }
  
  private static void runTestScripts() {
    int tests   = 0;
    int success = 0;
    int skipped = 0;
    
    for (File testScript : listTestScripts()) {
      tests++;
      
      TestInterpreterHost host = new TestInterpreterHost(testScript.getPath());
      host.run();

      if (host.passed()) success++;
      if (host.skipped()) skipped++;
    }
    
    System.out.println("Passed " + success + " out of " + (tests - skipped) + " tests.");
    if (skipped > 0) {
      System.out.println("(" + skipped + " disabled tests skipped.)");
    }
  }
  
  private static List<File> listTestScripts() {
    List<File> files = new ArrayList<File>();
    listTestScripts(new File("test"), files);
    
    return files;
  }
  
  private static void listTestScripts(File dir, List<File> files) {
    for (String fileName : dir.list()) {
      File file = new File(dir, fileName);
      if (fileName.endsWith(".mag")) {
        files.add(file);
      } else if (file.isDirectory()) {
        listTestScripts(file, files);
      }
    }
  }
}
