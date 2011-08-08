package com.stuffwithstuff.magpie;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.interpreter.ErrorException;
import com.stuffwithstuff.magpie.interpreter.Interpreter;
import com.stuffwithstuff.magpie.interpreter.Obj;
import com.stuffwithstuff.magpie.parser.MagpieParser;
import com.stuffwithstuff.magpie.parser.ParseException;
import com.stuffwithstuff.magpie.parser.TokenType;

public class Repl {
  Repl(Interpreter interpreter) {
    mInterpreter = interpreter;
  }
  
  public ReplResult readAndEvaluate(SourceReader reader) {
    MagpieParser parser = MagpieParser.create(reader,
        mInterpreter.getBaseModule().getGrammar());

    try {
      Expr expr = parser.parseExpression();
      parser.consume(TokenType.LINE);
      
      Obj result = mInterpreter.interpret(expr);
      String resultText;
      if (result == mInterpreter.nothing()) {
        resultText = null;
      } else {
        resultText = mInterpreter.evaluateToString(result);
      }
      return new ReplResult(resultText, false);
    } catch(ParseException ex) {
      return new ReplResult("Parse error: " + ex.getMessage(), true);
    } catch(ErrorException ex) {
      return new ReplResult(String.format("Uncaught %s: %s",
          ex.getError().getClassObj().getName(), ex.getError().getValue()),
          true);
    }
  }
  
  public boolean isKeyword(String name) {
    return mInterpreter.getBaseModule().getGrammar().isKeyword(name);
  }

  private final Interpreter mInterpreter;
}
