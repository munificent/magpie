package com.stuffwithstuff.magpie;

import java.io.IOException;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.Lexer;
import com.stuffwithstuff.magpie.parser.MagpieParser;

public class NiceRepl {
  public static void run() {
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
        NiceReplCharacterReader reader = new NiceReplCharacterReader(interpreter);
        Lexer lexer = new Lexer("REPL", reader);
        MagpieParser parser = new MagpieParser(lexer,
            interpreter.getParsewords(),
            interpreter.getKeywords());
        
        Expr expr = parser.parseExpression();
        
        // Set to white in case the expression does any printing.
        Term.set(Term.ForeColor.WHITE);
        
        String result = interpreter.evaluateToString(expr);
        
        if (result != null) {
          Term.set(Term.ForeColor.GRAY);
          System.out.print(" = ");
          Term.set(Term.ForeColor.GREEN);
          System.out.println(result);
          Term.restoreColor();
        }
      }
    } catch (QuitException e) {
      // Do nothing.
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
