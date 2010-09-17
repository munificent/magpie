package com.stuffwithstuff.magpie;

import java.io.*;
import java.util.*;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.InterpreterException;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;

public class Magpie {

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      // With no command line args, runs the REPL.
      System.out.println("magpie");
      System.out.println("------");
      runRepl();
    } else if (args.length == 1) {
      if (args[0].equals("test")) {
        System.out.println("Running test suite...");
        runTestScripts();
      } else {
        // One command line arg: load the script at that path and run it.
        runScript(args[0]);
      }
    }
  }
  
  private static void runRepl() {
    InputStreamReader converter = new InputStreamReader(System.in);
    BufferedReader in = new BufferedReader(converter);

    Interpreter interpreter = new Interpreter(new ScriptInterpreterHost());
    
    try {
      Script.loadBase(interpreter);
      
      while (true) {
        String code = "";
        while (true) {
          System.out.print("> ");
          String line = in.readLine();

          if (line.equals("quit")) break;
          
          if (code.length() > 0) code += "\n";
          code += line;

          if (line.equals("")) break;
        }

        if (code.equals("quit")) break;
        
        Lexer lexer = new Lexer("<repl>", code);
        MagpieParser parser = new MagpieParser(lexer);
        
        try {
          List<Expr> exprs = parser.parse();
          for (Expr expr : exprs) {
            String result = interpreter.evaluate(expr);
            System.out.print("= ");
            System.out.println(result);
          }
        } catch (ParseException ex) {
          System.out.println("! " + ex.toString());
        } catch (InterpreterException ex) {
          System.out.println("! " + ex.toString());
        }
      }
    } catch (IOException ex) {
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
      if (!testScript.getPath().contains("StaticArgs")) continue;
      
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
