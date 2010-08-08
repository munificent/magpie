package com.stuffwithstuff.magpie;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Magpie {

  /**
   * @param args
   */
  public static void main(String[] args) {
    runTestScripts();
    
    System.out.println("magpie");
    System.out.println("------");
    
    // TODO(bob): REPL is dead for now.
    /*
    InputStreamReader converter = new InputStreamReader(System.in);
    BufferedReader in = new BufferedReader(converter);

    Interpreter interpreter = new Interpreter();
    
    while (true) {
      try {
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
        
        Lexer lexer = new Lexer(code);
        MagpieParser parser = new MagpieParser(lexer);
        
        try {
          Expr expr = parser.parse();
          Obj result = interpreter.evaluate(expr);
          System.out.print("= ");
          System.out.println(result);
        } catch (ParseError err) {
          System.out.println("! " + err.toString());
        } catch (Exception ex) {
          System.out.println("! " + ex.toString());
        }
      } catch (IOException ex) {
        break;
      }
    }*/
  }
  
  private static void runTestScripts() {
    int tests   = 0;
    int success = 0;
    
    for (File testScript : listTestScripts()) {
      //if (!testScript.getPath().contains("BlockComments")) continue;
      
      tests++;
      if (runTestScript(testScript)) success++;
    }
    
    System.out.println("Passed " + success + " out of " + tests + " tests.");
  }
  
private static boolean runTestScript(File script) {      
    TestInterpreterHost host = new TestInterpreterHost(script.getPath());
    return host.run();
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
