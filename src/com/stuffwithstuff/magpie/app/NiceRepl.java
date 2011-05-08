package com.stuffwithstuff.magpie.app;

import com.stuffwithstuff.magpie.SourceReader;

public class NiceRepl extends Repl {
  @Override
  public void print(String text) {
    Term.set(Term.ForeColor.WHITE);
    System.out.print(text);
    Term.restoreColor();
  }
  
  @Override
  protected SourceReader createReader() {
    return new NiceReplCharacterReader(getInterpreter());
  }
  
  @Override
  protected void printResult(String result) {
    Term.set(Term.ForeColor.GRAY);
    System.out.print("= ");
    Term.set(Term.ForeColor.GREEN);
    System.out.println(result);
    Term.restoreColor();
  }
  
  @Override
  protected void printError(String message) {
    Term.set(Term.ForeColor.RED);
    System.out.println("! " + message);
    Term.restoreColor();
  }
}
