package com.stuffwithstuff.magpie.parser;

import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.util.Pair;

public class IfParser implements PrefixParser {
  @Override
  public Expr parse(MagpieParser parser, Token token) {
    // Parse the condition.
    Expr condition = parser.parseBlock("then").getKey();
    
    // Parse the then arm.
    parser.consume("then");
    Pair<Expr, Token> thenParse = parser.parseBlock("else", "end");
    Expr thenExpr = thenParse.getKey();
    
    // Parse the else body.
    boolean consumedEnd = (thenParse.getValue() != null) &&
        thenParse.getValue().isKeyword("end");
    
    Expr elseExpr;
    if (!consumedEnd && parser.match("else")) {
      elseExpr = parser.parseEndBlock();
    } else {
      elseExpr = null;
    }
    
    return Expr.if_(condition, thenExpr, elseExpr);
  }
}
