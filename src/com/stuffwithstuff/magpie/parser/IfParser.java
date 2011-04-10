package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.pattern.MatchCase;
import com.stuffwithstuff.magpie.ast.pattern.Pattern;
import com.stuffwithstuff.magpie.util.Pair;

public class IfParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    // Parse the condition.
    Expr condition = parser.parseExpressionOrBlock("then").getKey();
    
    // Parse the then arm.
    parser.consume("then");
    Pair<Expr, Token> thenParse = parser.parseExpressionOrBlock("else", "end");
    Expr thenExpr = thenParse.getKey();
    
    // Parse the else body.
    boolean consumedEnd = (thenParse.getValue() != null) &&
        thenParse.getValue().isKeyword("end");
    
    Expr elseExpr;
    if (!consumedEnd && parser.match("else")) {
      elseExpr = parser.parseExpressionOrBlock();
    } else {
      elseExpr = Expr.nothing();
    }
    
    // Convert it to a pattern matching expression.
    List<MatchCase> cases = new ArrayList<MatchCase>();
    cases.add(new MatchCase(Pattern.value(Expr.bool(true)), thenExpr));
    cases.add(new MatchCase(elseExpr));
    
    Expr truthy = Expr.call(condition.getPosition(), condition, "true?");
    
    return Expr.match(Position.surrounding(condition, elseExpr),
        truthy, cases);
  }
}
