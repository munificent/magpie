package com.stuffwithstuff.magpie;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import com.stuffwithstuff.magpie.ast.FunctionDefn;
import com.stuffwithstuff.magpie.interpreter.*;

public class Magpie {

  /**
   * @param args
   */
  public static void main(String[] args) {
    runTestScripts();
    
    System.out.println("magpie");
    System.out.println("------");
    
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
      tests++;
      if (runTestScript(testScript)) success++;
    }
    
    System.out.println("Passed " + success + " out of " + tests + " tests.");
  }
  
  private static boolean runTestScript(File script) {
    try {
      String source = readFile(script.getPath());
      
      TestInterpreterHost host = new TestInterpreterHost(script.getPath());
      return host.run(source);
    } catch (IOException ex) {
      System.out.println("FAIL " + script + ": IO error");
      return false;
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
  
  private static String readFile(String path) throws IOException {
    FileInputStream stream = new FileInputStream(path);

    try {
      InputStreamReader input = new InputStreamReader(stream, Charset
          .defaultCharset());
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
