package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;

public class ConjunctionParser extends InfixParser {
  @Override
  public Expr parse(MagpieParser parser, Expr left, Token token) {
    // Ignore a line after the conjunction.
    parser.match(TokenType.LINE);
    
    Expr right = parser.parseExpression(getStickiness());

    if (token.isKeyword("and")) {
      /*
      // TODO(bob): Generate unique symbol.
      String temp = "temp__";
      
      Expr variable = Expr.var(temp, left);
      
      List<MatchCase> cases = new ArrayList<MatchCase>();
      cases.add(new MatchCase(new ValuePattern(Expr.bool(true)), left));
      cases.add(new MatchCase(new VariablePattern("_", null), right));
      Expr match = Expr.match(Position.surrounding(left, right), 
          Expr.message(Expr.name(temp), "true?"), cases);
      
      return Expr.scope(Expr.block(variable, match));
      */
      // a and b
      // do
      //     var temp__ = a
      //     match temp__ true?
      //         case true then b
      //         case_     then temp__
      //     end
      // end
      return Expr.and(left, right);
    } else {
      return Expr.or(left, right);
    }
  }
  
  @Override
  public int getStickiness() { return 60; }
}
