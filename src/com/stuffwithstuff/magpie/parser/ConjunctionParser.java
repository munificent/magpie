package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.ValuePattern;
import com.stuffwithstuff.magpie.ast.pattern.VariablePattern;

public class ConjunctionParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Ignore a line after the conjunction.
    parser.match(TokenType.LINE);
    
    Expr right = parser.parseExpression(getStickiness());

    // Desugar it to a match:
    // (a b) and (c d)
    // do
    //     var temp__ = a b
    //     match temp__ true?
    //         case true then c d
    //         case _    then temp__
    //     end
    // end
    //
    // (a b) or (c d)
    // do
    //     var temp__ = a b
    //     match temp__ true?
    //         case true then temp__
    //         case _    then c d
    //     end
    // end
    String temp = parser.generateName();
    
    Expr variable = Expr.var(temp, left);
    
    List<MatchCase> cases = new ArrayList<MatchCase>();

    if (token.isKeyword("and")) {
      cases.add(new MatchCase(new ValuePattern(Expr.bool(true)), right));
      cases.add(new MatchCase(new VariablePattern("_", null), Expr.name(temp)));
    } else {
      cases.add(new MatchCase(new ValuePattern(Expr.bool(true)), Expr.name(temp)));
      cases.add(new MatchCase(new VariablePattern("_", null), right));
    }
    
    Expr match = Expr.match(Position.surrounding(left, right), 
        Expr.message(Expr.name(temp), "true?"), cases);
    return Expr.scope(Expr.block(variable, match));
  }
  
  @Override
  public int getStickiness() { return 60; }
}
