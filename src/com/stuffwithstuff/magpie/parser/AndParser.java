package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.interpreter.Name;

/**
 * Parses an "and" operator.
 */
public class AndParser implements InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    Expr right = parser.parsePrecedence(Precedence.LOGICAL);
    
    // Desugar to a pattern match:
    //
    // match (val temp__ = `left) isTrue
    //     case true then `right
    //     else temp__
    // end
    // TODO(bob): This desugaring should be done in a later pass.
    String temp = parser.generateName();
    Expr value = Expr.var(left.getPosition(), false, temp, left);
    Expr truthCheck = Expr.call(left.getPosition(), value, Name.IS_TRUE);
    
    List<MatchCase> cases = new ArrayList<MatchCase>();
    cases.add(new MatchCase(Pattern.value(Expr.bool(true)), right));
    cases.add(new MatchCase(Expr.name(temp)));
    
    return Expr.match(left.getPosition().union(right.getPosition()),
        truthCheck, cases);
  }
  
  @Override
  public int getPrecedence() { return Precedence.LOGICAL; }
}
