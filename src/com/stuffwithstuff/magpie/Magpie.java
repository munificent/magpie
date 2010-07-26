package com.stuffwithstuff.magpie;

import java.io.*;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.*;

public class Magpie {

  /**
   * @param args
   */
  public static void main(String[] args) {
    System.out.println("magpie");
    System.out.println("------");
    
    InputStreamReader converter = new InputStreamReader(System.in);
    BufferedReader in = new BufferedReader(converter);

    Interpreter interpreter = new Interpreter();
    
    while (true) {
      try {
        System.out.print("> ");
        String line = in.readLine();
        if (line.equals("quit")) break;
        
        Lexer lexer = new Lexer(line);
        MagpieParser parser = new MagpieParser(lexer);
        
        try {
          Expr expr = parser.parse();
          Obj result = interpreter.evaluate(expr);
          System.out.print("= ");
          System.out.println(result);
        } catch (Error err) {
          System.out.println("! " + err.toString());
        }
        
      } catch (IOException ex) {
        break;
      }
    }
  }
}
