package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Callable;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Scope;
import com.stuffwithstuff.magpie.intrinsic.IntrinsicCallable;
import com.stuffwithstuff.magpie.intrinsic.IntrinsicLoader;
import com.stuffwithstuff.magpie.intrinsic.MethodWrapper;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.util.Pair;

public class Magpie {
  public Magpie(MagpieHost host) {
    mInterpreter = new Interpreter(host);
  }
  
  public String run(SourceFile source) {
    try {
      mInterpreter.interpret(source);
      return null;
    } catch(ErrorException ex) {
      return String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue());
    }
  }
  
  public Repl createRepl() {
    return new Repl(mInterpreter);
  }
  
  public void defineMethod(String signature, String doc, Method method) {
    MagpieParser parser = new MagpieParser(signature);
    Pair<String, Pattern> parsed = parser.parseSignature();

    String name = parsed.getKey();
    Pattern pattern = parsed.getValue();
    
    Scope scope = mInterpreter.getBaseModule().getScope();
    
    // Construct the method.
    Callable callable = new IntrinsicCallable(pattern, doc, new MethodWrapper(method), scope);
    
    // Register it.
    scope.define(name, callable);
  }
  
  @SuppressWarnings("rawtypes")
  public void defineMethods(Class javaClass) {
    IntrinsicLoader.register(javaClass,
        mInterpreter.getBaseModule().getScope());
  }
  
  private final Interpreter mInterpreter;
}
