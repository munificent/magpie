package com.stuffwithstuff.magpie;

import java.io.*;
import com.stuffwithstuff.magpie.ast.Expr;

public class Magpie {

  /**
   * @param args
   */
  public static void main(String[] args) {
    System.out.println("magpie");
    System.out.println("------");
    
    InputStreamReader converter = new InputStreamReader(System.in);
    BufferedReader in = new BufferedReader(converter);

    while (true) {
      try {
        String line = in.readLine();
        if (line.equals("quit")) break;
        
        Lexer lexer = new Lexer(line);
        MagpieParser parser = new MagpieParser(lexer);
        
        try {
        Expr result = parser.parse();
        System.out.println(result);
        } catch (Error err) {
          System.out.println("!!! " + err.toString());
        }
        
      } catch (IOException ex) {
        break;
      }
    }
  }
}
