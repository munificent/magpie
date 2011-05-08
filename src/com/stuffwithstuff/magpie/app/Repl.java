package com.stuffwithstuff.magpie.app;

import com.stuffwithstuff.magpie.SourceReader;
import com.stuffwithstuff.magpie.MagpieHost;
import com.stuffwithstuff.magpie.SourceFile;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.TokenType;

public class Repl implements MagpieHost {
  public Repl() {
    mInterpreter = new Interpreter(this);
  }
  
  public void run() {
    System.out.println();
    System.out.println("      _/Oo>");
    System.out.println("     /__/     magpie v0.0.0");
    System.out.println("____//hh___________________");
    System.out.println("   //");
    System.out.println();
    System.out.println("Type 'quit()' and press <Enter> to exit.");
    
    try {
      
      while (true) {
        MagpieParser parser = MagpieParser.create(createReader(),
            mInterpreter.getBaseModule().getGrammar());
        
        try {
          Expr expr = parser.parseExpression();
          parser.consume(TokenType.LINE);
          
          Obj result = mInterpreter.interpret(expr);
          if (result != mInterpreter.nothing()) {
            String text = mInterpreter.evaluateToString(result);
            
            // Indent the lines.
            text = text.replace("\n", "\n  ");
            
            printResult(text);
          }
        } catch(ParseException ex) {
          printError("Parse error: " + ex.getMessage());
        } catch(ErrorException ex) {
          printError(String.format("Uncaught %s: %s",
              ex.getError().getClassObj().getName(), ex.getError().getValue()));
        }
      }
    } catch (QuitException e) {
      // Do nothing.
    }
  }

  @Override
  public void print(String text) {
    System.out.print(text);
  }

  @Override
  public SourceFile loadModule(String name) {
    return MagpieApp.loadModule(name);
  }

  protected SourceReader createReader() {
    return new ReplReader();
  }
  
  protected void printResult(String result) {
    System.out.print("= ");
    System.out.println(result);
  }
  
  protected void printError(String message) {
    System.out.println("! " + message);
  }
  
  protected Interpreter getInterpreter() {
    return mInterpreter;
  }
  
  private final Interpreter mInterpreter;
}
