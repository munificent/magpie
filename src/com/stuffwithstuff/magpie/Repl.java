package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.InterpreterHost;
import com.stuffwithstuff.magpie.interpreter.ModuleInfo;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.interpreter.QuitException;
import com.stuffwithstuff.magpie.parser.CharacterReader;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.TokenType;

public class Repl implements InterpreterHost {
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
        MagpieParser parser = mInterpreter.createParser(createReader());
        
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
  public boolean allowTopLevelRedefinition() {
    return true;
  }

  @Override
  public void print(String text) {
    System.out.print(text);
  }

  @Override
  public ModuleInfo loadModule(String name) {
    return Script.loadModule(name);
  }

  protected CharacterReader createReader() {
    return new ReplCharacterReader();
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
