package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;

public class Magpie {
  public Magpie(MagpieHost host) {
    mInterpreter = new Interpreter(host);
  }
  
  public void run(SourceFile source) {
    try {
      mInterpreter.interpret(source);
    } catch(ErrorException ex) {
      mInterpreter.print(String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue()));
    }
  }
  
  private final Interpreter mInterpreter;
}
