package com.stuffwithstuff.magpie.app;

import com.stuffwithstuff.magpie.Def;
import com.stuffwithstuff.magpie.Magpie;
import com.stuffwithstuff.magpie.Method;
import com.stuffwithstuff.magpie.Repl;
import com.stuffwithstuff.magpie.ReplResult;

public class ConsoleRepl extends MagpieAppHost {
  public ConsoleRepl() {
    mMagpie = new Magpie(this);
    
    mMagpie.defineMethods(ReplMethods.class);
    mMagpie.defineMethod("printString(s is String)", new PrintString());
    
    mRepl = mMagpie.createRepl();
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
        ReplResult result = mRepl.readAndEvaluate(createReader(mRepl));
        
        // Indent the lines.
        String text = result.getText().replace("\n", "\n  ");
        if (result.isError()) {
          printError(text);
        } else {
          printResult(text);
        }
      }
    } catch (QuitException e) {
      // Do nothing.
    }
  }

  public void print(String text) {
    System.out.print(text);
  }

  protected ReplReader createReader(Repl repl) {
    return new ReplReader(repl);
  }
  
  protected void printResult(String result) {
    System.out.print("= ");
    System.out.println(result);
  }
  
  protected void printError(String message) {
    System.out.println("! " + message);
  }
  
  protected Repl getRepl() {
    return mRepl;
  }
  
  @Def("prints(text is String)")
  private class PrintString implements Method {
    public Object call(Object left, Object right) {
      print(right.toString());
      return null;
    }
  }
  
  private final Magpie mMagpie;
  private final Repl mRepl;
}
